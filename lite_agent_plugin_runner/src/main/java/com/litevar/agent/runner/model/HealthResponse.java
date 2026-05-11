package com.litevar.agent.runner.model;


/**
 * Runner health response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class HealthResponse {

    private boolean paired;
    private boolean dockerOk;

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    public boolean isDockerOk() {
        return dockerOk;
    }

    public void setDockerOk(boolean dockerOk) {
        this.dockerOk = dockerOk;
    }
}
