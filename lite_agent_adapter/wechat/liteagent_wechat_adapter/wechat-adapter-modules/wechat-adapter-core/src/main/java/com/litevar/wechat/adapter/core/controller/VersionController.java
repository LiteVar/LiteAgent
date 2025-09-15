package com.litevar.wechat.adapter.core.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import com.litevar.wechat.adapter.common.core.config.WechatAdapterProjectProperties;
import com.litevar.wechat.adapter.common.core.web.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  版本控制器
 * @author Teoan
 * @since 2025/8/13 11:23
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/version")
@Tag(name = "版本信息", description = "版本信息接口")
public class VersionController {

   private final WechatAdapterProjectProperties wechatAdapterProjectProperties;

    /**
     * 用于读取版本号，也可用于测试服务通断
     */
    @GetMapping()
    @Operation(summary = "获取版本号", description = "可用于测试服务通断")
    @SaIgnore
    public R<String> getVersionInfo() {
        return R.ok(StrUtil.format("当前版本为：{}", wechatAdapterProjectProperties.getVersion()));
    }
}