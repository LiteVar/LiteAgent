package com.litevar.dingtalk.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DingtalkAdapterServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DingtalkAdapterServerApplication.class, args);
    }

}