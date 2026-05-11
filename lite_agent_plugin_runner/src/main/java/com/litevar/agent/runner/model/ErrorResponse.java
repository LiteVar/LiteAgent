package com.litevar.agent.runner.model;


/**
 * Runner error response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class ErrorResponse {

    private String code;
    private String message;
    private String detail;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, String detail) {
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
