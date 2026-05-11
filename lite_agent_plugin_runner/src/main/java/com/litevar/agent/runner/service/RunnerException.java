package com.litevar.agent.runner.service;


/**
 * Runner exception.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class RunnerException extends RuntimeException {

    private final RunnerErrorCode errorCode;
    private final String detail;

    public RunnerException(RunnerErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public RunnerErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
