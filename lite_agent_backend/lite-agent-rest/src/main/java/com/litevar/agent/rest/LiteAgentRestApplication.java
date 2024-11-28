package com.litevar.agent.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@EnableAsync
@SpringBootApplication
@EnableMongoAuditing
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableMongoRepositories(basePackages = "com.litevar.agent.base")
@ComponentScan("com.litevar.agent")
public class LiteAgentRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteAgentRestApplication.class, args);
    }

}
