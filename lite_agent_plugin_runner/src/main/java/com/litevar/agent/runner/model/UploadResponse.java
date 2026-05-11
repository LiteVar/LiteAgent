package com.litevar.agent.runner.model;


/**
 * Upload response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class UploadResponse {

    private boolean success;

    public UploadResponse() {
    }

    public UploadResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
