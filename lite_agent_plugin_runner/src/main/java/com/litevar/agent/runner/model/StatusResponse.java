package com.litevar.agent.runner.model;


/**
 * Plugin status response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class StatusResponse {

    private String containerId;
    private boolean running;
    private Integer hostPort;

    public StatusResponse() {
    }

    public StatusResponse(String containerId, boolean running, Integer hostPort) {
        this.containerId = containerId;
        this.running = running;
        this.hostPort = hostPort;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public void setHostPort(Integer hostPort) {
        this.hostPort = hostPort;
    }
}
