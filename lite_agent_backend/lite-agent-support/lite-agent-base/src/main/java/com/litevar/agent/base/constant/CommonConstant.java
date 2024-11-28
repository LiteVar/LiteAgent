package com.litevar.agent.base.constant;

import org.springframework.http.HttpHeaders;

/**
 * @author uncle
 * @since 2024/7/4 11:26
 */
public interface CommonConstant {
    String JWT_SECRET = "liteAgent@2024#!";
    String JWT_TOKEN_PREFIX = "Bearer";

    String HEADER_AUTH = HttpHeaders.AUTHORIZATION;
    String HEADER_WORKSPACE_ID = "Workspace-id";
}