package com.litevar.agent.runner.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LoadImageCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.litevar.agent.runner.config.RunnerProperties;
import com.litevar.agent.runner.model.PluginRuntime;
import com.litevar.agent.runner.service.RunnerErrorCode;
import com.litevar.agent.runner.service.RunnerException;
import com.litevar.agent.runner.service.RunnerStateStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Docker operations for plugin runner.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class DockerService {

    // 插件容器内固定端口。
    private static final int PLUGIN_CONTAINER_PORT = 8888;
    // 容器内插件持久化数据目录。
    private static final String PLUGIN_DATA_PATH = "/data/lite-agent-plugin";
    // 容器内 tmpfs 挂载目录。
    private static final String TEMP_PATH = "/tmp";
    // 容器内存上限。
    private static final long MEMORY_LIMIT_BYTES = 512L * 1024 * 1024;
    // 容器 CPU 配额。
    private static final double CPU_LIMIT = 0.5;
    // Docker 使用 NanoCPUs 表示 CPU 配额。
    private static final long CPU_NANOS = (long) (CPU_LIMIT * 1_000_000_000L);
    // 限制容器内最大进程数，防止 fork 炸弹。
    private static final long PIDS_LIMIT = 512;
    // 容器 rootfs 只读。
    private static final boolean READ_ONLY_ROOTFS = true;
    // /tmp tmpfs 大小。
    private static final long TMPFS_SIZE_BYTES = 64L * 1024 * 1024;
    // 容器日志单文件大小上限。
    private static final String LOG_MAX_SIZE = "10m";
    // 容器日志保留文件数。
    private static final String LOG_MAX_FILE = "3";

    private final DockerClient dockerClient;
    private final RunnerProperties properties;
    private final RunnerStateStore stateStore;

    public DockerService(DockerClient dockerClient, RunnerProperties properties, RunnerStateStore stateStore) {
        this.dockerClient = dockerClient;
        this.properties = properties;
        this.stateStore = stateStore;
    }

    public boolean isDockerOk() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public ContainerRunResult runContainer(String pluginId, Path packageFile) {
        String createdId = null;
        try {
            String containerName = containerName(pluginId);
            InspectContainerResponse existing = findContainer(containerName);
            ExposedPort exposedPort = ExposedPort.tcp(PLUGIN_CONTAINER_PORT);
            if (existing != null) {
                Boolean running = existing.getState().getRunning();
                if (Boolean.TRUE.equals(running)) {
                    int resolvedPort = resolveHostPort(existing, exposedPort);
                    if (resolvedPort == 0) {
                        resolvedPort = resolveHostPort(existing, null);
                    }
                    return new ContainerRunResult(existing.getId(), resolvedPort);
                }
                dockerClient.startContainerCmd(existing.getId()).exec();
                InspectContainerResponse info = dockerClient.inspectContainerCmd(existing.getId()).exec();
                int resolvedPort = resolveHostPort(info, exposedPort);
                if (resolvedPort == 0) {
                    resolvedPort = resolveHostPort(info, null);
                }
                return new ContainerRunResult(existing.getId(), resolvedPort);
            }
            //docker load image
            String imageName = loadImage(packageFile);
            Ports ports = new Ports();
            Integer desiredHostPort = resolveDesiredHostPort(pluginId);
            if (desiredHostPort != null) {
                ports.bind(exposedPort, Ports.Binding.bindPort(desiredHostPort));
            } else {
                int allocatedPort = stateStore.allocateHostPort();
                ports.bind(exposedPort, Ports.Binding.bindPort(allocatedPort));
            }

            Path pluginDataDir = resolvePluginDataDir(pluginId);
            ensureDirectory(pluginDataDir);
            Path hostPluginDataDir = resolveHostPluginDataDir(pluginId);
            Bind dataBind = new Bind(hostPluginDataDir.toString(), new Volume(PLUGIN_DATA_PATH), AccessMode.rw);

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(ports)
                    .withCapDrop(Capability.ALL)
                    .withSecurityOpts(List.of("no-new-privileges"))
                    .withReadonlyRootfs(READ_ONLY_ROOTFS)
                    .withPidsLimit(PIDS_LIMIT)
                    .withMemory(MEMORY_LIMIT_BYTES)
                    .withNanoCPUs(CPU_NANOS)
                    .withTmpFs(Map.of(TEMP_PATH, "rw,noexec,nosuid,size=" + TMPFS_SIZE_BYTES))
                    .withBinds(dataBind)
                    .withLogConfig(new LogConfig(LogConfig.LoggingType.JSON_FILE,
                            Map.of("max-size", LOG_MAX_SIZE, "max-file", LOG_MAX_FILE)));

            //docker create
            CreateContainerResponse created = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withExposedPorts(exposedPort)
                    .withHostConfig(hostConfig)
                    .exec();
            createdId = created.getId();
            // docker start
            dockerClient.startContainerCmd(createdId).exec();
            // docker inspect
            InspectContainerResponse info = dockerClient.inspectContainerCmd(createdId).exec();
            int resolvedPort = resolveHostPort(info, exposedPort);
            return new ContainerRunResult(createdId, resolvedPort);
        } catch (RunnerException ex) {
            throw ex;
        } catch (Exception ex) {
            if (createdId != null) {
                try {
                    dockerClient.removeContainerCmd(createdId).withForce(true).exec();
                } catch (Exception ignore) {
                }
            }
            throw new RunnerException(RunnerErrorCode.START_FAILED, ex.getMessage());
        }
    }

    public void stopContainer(String pluginId) {
        String containerName = containerName(pluginId);
        InspectContainerResponse existing = findContainer(containerName);
        if (existing == null) {
            throw new RunnerException(RunnerErrorCode.CONTAINER_NOT_FOUND, null);
        }
        try {
            //docker stop
            dockerClient.stopContainerCmd(existing.getId()).withTimeout(5).exec();
        } catch (NotFoundException ex) {
            throw new RunnerException(RunnerErrorCode.CONTAINER_NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.START_FAILED, ex.getMessage());
        }
    }

    public ContainerStatus status(String pluginId) {
        InspectContainerResponse existing = findContainer(containerName(pluginId));
        if (existing == null) {
            throw new RunnerException(RunnerErrorCode.CONTAINER_NOT_FOUND, null);
        }
        boolean running = Boolean.TRUE.equals(existing.getState().getRunning());
        Integer hostPort = resolveHostPort(existing, null);
        return new ContainerStatus(existing.getId(), running, hostPort);
    }

    public String logs(String pluginId, Integer tail) {
        InspectContainerResponse existing = findContainer(containerName(pluginId));
        if (existing == null) {
            throw new RunnerException(RunnerErrorCode.CONTAINER_NOT_FOUND, null);
        }
        int tailCount = tail == null || tail <= 0 ? 200 : tail;
        StringBuilder sb = new StringBuilder();
        try {
            dockerClient.logContainerCmd(existing.getId())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tailCount)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(Frame item) {
                            sb.append(new String(item.getPayload(), StandardCharsets.UTF_8));
                        }
                    }).awaitCompletion();
            return sb.toString();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    private String loadImage(Path packageFile) {
        if (!Files.exists(packageFile)) {
            throw new RunnerException(RunnerErrorCode.PACKAGE_NOT_FOUND, "package missing");
        }
        try (InputStream inputStream = Files.newInputStream(packageFile)) {
            ImageLoadCallback callback = new ImageLoadCallback();
            dockerClient.loadImageAsyncCmd(inputStream).exec(callback).awaitCompletion();
            if (!StringUtils.hasText(callback.getImageName())) {
                throw new RunnerException(RunnerErrorCode.START_FAILED, "image name not found");
            }
            return callback.getImageName();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RunnerException(RunnerErrorCode.START_FAILED, ex.getMessage());
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.START_FAILED, ex.getMessage());
        }
    }

    private InspectContainerResponse findContainer(String name) {
        //docker ps -a
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        String target = "/" + name;
        for (Container container : containers) {
            String[] names = container.getNames();
            if (names != null) {
                for (String containerName : names) {
                    if (target.equals(containerName)) {
                        //存在相同的容器名字
                        //docker inspect
                        return dockerClient.inspectContainerCmd(container.getId()).exec();
                    }
                }
            }
        }
        return null;
    }

    private Integer resolveDesiredHostPort(String pluginId) {
        PluginRuntime runtime = stateStore.getRuntime(pluginId);
        if (runtime == null) {
            return null;
        }
        int hostPort = runtime.getHostPort();
        return hostPort > 0 ? hostPort : null;
    }

    private Path resolvePluginDataDir(String pluginId) {
        return Path.of(properties.getDataDir(), "plugins", pluginId);
    }

    private Path resolveHostPluginDataDir(String pluginId) {
        String baseDir = properties.getHostDataDir();
        if (!StringUtils.hasText(baseDir)) {
            baseDir = properties.getDataDir();
        }
        return Path.of(baseDir, "plugins", pluginId);
    }

    private void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.START_FAILED, ex.getMessage());
        }
    }

    private int resolveHostPort(InspectContainerResponse info, ExposedPort exposedPort) {
        Ports ports = info.getNetworkSettings().getPorts();
        if (ports == null) {
            return 0;
        }
        Map<ExposedPort, Ports.Binding[]> bindings = ports.getBindings();
        if (bindings == null || bindings.isEmpty()) {
            return 0;
        }
        if (exposedPort != null) {
            Ports.Binding[] bindingArr = bindings.get(exposedPort);
            if (bindingArr != null && bindingArr.length > 0) {
                return Integer.parseInt(bindingArr[0].getHostPortSpec());
            }
            return 0;
        }
        Ports.Binding[] anyBinding = bindings.values().stream()
                .filter(arr -> arr != null && arr.length > 0)
                .findFirst()
                .orElse(null);
        if (anyBinding == null) {
            return 0;
        }
        return Integer.parseInt(anyBinding[0].getHostPortSpec());
    }

    private String containerName(String pluginId) {
        //前缀+插件ID
        return "lite-agent-plugin-" + pluginId;
    }

    public static class ContainerRunResult {
        private final String containerId;
        private final int hostPort;

        public ContainerRunResult(String containerId, int hostPort) {
            this.containerId = containerId;
            this.hostPort = hostPort;
        }

        public String getContainerId() {
            return containerId;
        }

        public int getHostPort() {
            return hostPort;
        }
    }

    public static class ContainerStatus {
        private final String containerId;
        private final boolean running;
        private final Integer hostPort;

        public ContainerStatus(String containerId, boolean running, Integer hostPort) {
            this.containerId = containerId;
            this.running = running;
            this.hostPort = hostPort;
        }

        public String getContainerId() {
            return containerId;
        }

        public boolean isRunning() {
            return running;
        }

        public Integer getHostPort() {
            return hostPort;
        }
    }

    private static class ImageLoadCallback extends LoadImageCallback {

        private String imageName;

        @Override
        public void onNext(LoadResponseItem item) {
            String stream = item.getStream();
            if (stream != null) {
                String trimmed = stream.trim();
                if (trimmed.startsWith("Loaded image:")) {
                    imageName = trimmed.substring("Loaded image:".length()).trim();
                } else if (trimmed.startsWith("Loaded image ID:")) {
                    imageName = trimmed.substring("Loaded image ID:".length()).trim();
                }
            }
            super.onNext(item);
        }

        public String getImageName() {
            return imageName;
        }
    }
}
