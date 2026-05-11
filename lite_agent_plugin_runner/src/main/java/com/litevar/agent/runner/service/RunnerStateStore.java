package com.litevar.agent.runner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.runner.config.RunnerProperties;
import com.litevar.agent.runner.model.PluginRuntime;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner state storage.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class RunnerStateStore {

    private final ObjectMapper objectMapper;
    private final Path dataDir;
    private final Path runtimeFile;
    private final Path portFile;

    private final Map<String, PluginRuntime> runtimeRecords = new HashMap<>();
    private final List<Integer> portRecords = new ArrayList<>();

    public RunnerStateStore(ObjectMapper objectMapper, RunnerProperties properties) {
        this.objectMapper = objectMapper;
        this.dataDir = Path.of(properties.getDataDir());
        this.runtimeFile = dataDir.resolve("runtime.json");
        this.portFile = dataDir.resolve("ports.json");
        init();
    }

    public synchronized PluginRuntime getRuntime(String pluginId) {
        return runtimeRecords.get(pluginId);
    }

    public synchronized void saveRuntime(PluginRuntime runtime) {
        runtimeRecords.put(runtime.getPluginId(), runtime);
        writeFile(runtimeFile, runtimeRecords);
    }

    public synchronized void removeRuntime(String pluginId) {
        runtimeRecords.remove(pluginId);
        writeFile(runtimeFile, runtimeRecords);
    }

    public synchronized int allocateHostPort() {
        int nextPort = resolveNextHostPort();
        portRecords.add(nextPort);
        writeFile(portFile, portRecords);
        return nextPort;
    }

    private void init() {
        try {
            Files.createDirectories(dataDir);
            if (Files.exists(runtimeFile)) {
                Map<String, PluginRuntime> data = objectMapper.readValue(runtimeFile.toFile(),
                        new TypeReference<Map<String, PluginRuntime>>() {
                        });
                runtimeRecords.putAll(data);
            }
            if (Files.exists(portFile)) {
                List<Integer> data = objectMapper.readValue(portFile.toFile(),
                        new TypeReference<List<Integer>>() {
                        });
                portRecords.addAll(data);
            }
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    private void writeFile(Path path, Object value) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    private int resolveNextHostPort() {
        int maxPort = 57999;
        for (Integer port : portRecords) {
            if (port != null && port > maxPort) {
                maxPort = port;
            }
        }
        return maxPort + 1;
    }
}
