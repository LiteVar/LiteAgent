package com.litevar.agent.runner.service;

import org.springframework.http.HttpStatus;

/**
 * Runner error codes.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public enum RunnerErrorCode {
    INVALID_PARAM(HttpStatus.BAD_REQUEST, "INVALID_PARAM", "invalid param"),
    KEY_NOT_READY(HttpStatus.UNAUTHORIZED, "KEY_NOT_READY", "key not ready"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "invalid signature"),
    DOCKER_NOT_AVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "DOCKER_NOT_AVAILABLE", "docker not available"),
    START_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "START_FAILED", "docker start failed"),
    CONTAINER_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTAINER_NOT_FOUND", "container not found"),
    CONTAINER_ALREADY_RUNNING(HttpStatus.CONFLICT, "CONTAINER_ALREADY_RUNNING", "container already running"),
    PACKAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PACKAGE_NOT_FOUND", "package not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "internal error");

    private final HttpStatus status;
    private final String code;
    private final String message;

    RunnerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
