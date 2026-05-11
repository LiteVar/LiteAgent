package com.litevar.agent.runner.config;

import com.github.dockerjava.api.DockerClient;
import com.litevar.agent.runner.service.RunnerErrorCode;
import com.litevar.agent.runner.service.RunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runner startup checks.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class RunnerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RunnerBootstrap.class);
    private final DockerClient dockerClient;

    public RunnerBootstrap(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        //项目启动检查是否安装docker
        try {
            dockerClient.pingCmd().exec();
        } catch (Exception ex) {
            String message = "Docker is not reachable. Please check the Docker daemon and runtime environment.";
            log.error(message, ex);
            throw new RunnerException(RunnerErrorCode.DOCKER_NOT_AVAILABLE, message);
        }
    }
}
