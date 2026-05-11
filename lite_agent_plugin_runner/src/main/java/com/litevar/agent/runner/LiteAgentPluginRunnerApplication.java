package com.litevar.agent.runner;

import com.litevar.agent.runner.config.RunnerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Plugin runner application entry.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@SpringBootApplication
@EnableConfigurationProperties(RunnerProperties.class)
public class LiteAgentPluginRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteAgentPluginRunnerApplication.class, args);
    }
}
