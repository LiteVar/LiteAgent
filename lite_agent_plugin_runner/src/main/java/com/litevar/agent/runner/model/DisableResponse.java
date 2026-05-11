package com.litevar.agent.runner.model;


/**
 * Plugin disable response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class DisableResponse {

    private String status;

    public DisableResponse() {
    }

    public DisableResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
