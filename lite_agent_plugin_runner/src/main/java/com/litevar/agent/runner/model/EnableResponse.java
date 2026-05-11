package com.litevar.agent.runner.model;


/**
 * Plugin enable response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class EnableResponse {

    private String containerId;
    private int hostPort;

    public EnableResponse() {
    }

    public EnableResponse(String containerId, int hostPort) {
        this.containerId = containerId;
        this.hostPort = hostPort;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public int getHostPort() {
        return hostPort;
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
    }
}
