package com.litevar.agent.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@EnableAsync(proxyTargetClass = true)
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan("com.litevar.agent")
@EntityScan("com.litevar.agent")
public class LiteAgentRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteAgentRestApplication.class, args);
    }

}
