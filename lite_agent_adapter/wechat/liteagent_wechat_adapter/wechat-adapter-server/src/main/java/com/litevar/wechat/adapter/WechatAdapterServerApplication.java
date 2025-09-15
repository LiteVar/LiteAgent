package com.litevar.wechat.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WechatAdapterServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatAdapterServerApplication.class, args);
    }

}