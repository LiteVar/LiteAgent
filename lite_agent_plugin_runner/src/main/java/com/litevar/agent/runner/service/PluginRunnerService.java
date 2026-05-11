package com.litevar.agent.runner.service;

import com.litevar.agent.runner.docker.DockerService;
import com.litevar.agent.runner.model.EnableResponse;
import com.litevar.agent.runner.model.PluginRuntime;
import com.litevar.agent.runner.model.StatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

/**
 * Plugin runner service.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PluginRunnerService {

    private final DockerService dockerService;
    private final PackageService packageService;
    private final PluginKeyService pluginKeyService;
    private final RunnerStateStore stateStore;

    public PluginRunnerService(DockerService dockerService, PackageService packageService, PluginKeyService pluginKeyService,
                               RunnerStateStore stateStore) {
        this.dockerService = dockerService;
        this.packageService = packageService;
        this.pluginKeyService = pluginKeyService;
        this.stateStore = stateStore;
    }

    public EnableResponse enable(String pluginId, String packageUrl) {
        if (!packageService.hasPackage(pluginId)) {
            if (StringUtils.hasText(packageUrl)) {
                packageService.downloadPackage(pluginId, packageUrl);
            } else {
                throw new RunnerException(RunnerErrorCode.PACKAGE_NOT_FOUND, "package missing and no url provided");
            }
        }

        Path packageFile = packageService.getLatestPackage(pluginId);
        pluginKeyService.ensureKeyFile(pluginId);
        DockerService.ContainerRunResult result = dockerService.runContainer(pluginId, packageFile);
        int resolvedPort = result.getHostPort();
        PluginRuntime runtime = new PluginRuntime(pluginId, result.getContainerId(), resolvedPort,
                System.currentTimeMillis());
        stateStore.saveRuntime(runtime);
        return new EnableResponse(result.getContainerId(), resolvedPort);
    }

    public void disable(String pluginId) {
        dockerService.stopContainer(pluginId);
        PluginRuntime runtime = stateStore.getRuntime(pluginId);
        if (runtime != null) {
            runtime.setUpdatedAt(System.currentTimeMillis());
            stateStore.saveRuntime(runtime);
        }
    }

    public StatusResponse status(String pluginId) {
        DockerService.ContainerStatus status = dockerService.status(pluginId);
        return new StatusResponse(status.getContainerId(), status.isRunning(), status.getHostPort());
    }

    public String logs(String pluginId, Integer tail) {
        return dockerService.logs(pluginId, tail);
    }
}