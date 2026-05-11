package com.litevar.agent.runner.controller;

import com.litevar.agent.runner.docker.DockerService;
import com.litevar.agent.runner.model.*;
import com.litevar.agent.runner.service.PackageService;
import com.litevar.agent.runner.service.PairingService;
import com.litevar.agent.runner.service.PluginRunnerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Runner API controller.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@RestController
@RequestMapping("/runner")
public class RunnerController {
    private final PairingService pairingService;
    private final PackageService packageService;
    private final PluginRunnerService pluginRunnerService;
    private final DockerService dockerService;

    public RunnerController(PairingService pairingService, PackageService packageService,
                            PluginRunnerService pluginRunnerService, DockerService dockerService) {
        this.pairingService = pairingService;
        this.packageService = packageService;
        this.pluginRunnerService = pluginRunnerService;
        this.dockerService = dockerService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        HealthResponse response = new HealthResponse();
        response.setPaired(pairingService.isKeyReady());
        response.setDockerOk(dockerService.isDockerOk());
        return response;
    }

    @PostMapping("/plugins/{pluginId}/package")
    public UploadResponse upload(@PathVariable String pluginId, @RequestPart("file") MultipartFile file) {
        packageService.savePackage(pluginId, file);
        return new UploadResponse(true);
    }

    @PostMapping("/plugins/{pluginId}/enable")
    public EnableResponse enable(@PathVariable String pluginId, @RequestBody(required = false) EnableRequest request) {
        String url = request != null ? request.getPackageUrl() : null;
        return pluginRunnerService.enable(pluginId, url);
    }

    @PostMapping("/plugins/{pluginId}/disable")
    public DisableResponse disable(@PathVariable String pluginId) {
        pluginRunnerService.disable(pluginId);
        return new DisableResponse("stopped");
    }

    @GetMapping("/plugins/{pluginId}/status")
    public StatusResponse status(@PathVariable String pluginId) {
        return pluginRunnerService.status(pluginId);
    }

    @GetMapping("/plugins/{pluginId}/logs")
    public LogsResponse logs(@PathVariable String pluginId, @RequestParam(required = false) Integer tail) {
        return new LogsResponse(pluginRunnerService.logs(pluginId, tail));
    }
}