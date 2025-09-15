package com.litevar.dingtalk.adapter.common.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Teoan
 * @since 2025/8/20 14:38
 */
// 扫描cn.hutool.extra.spring包下所有类并注册之
@ComponentScan(basePackages={"cn.hutool.extra.spring"})
@Import(cn.hutool.extra.spring.SpringUtil.class)
@Configuration
public class HutoolConfigure {
}
