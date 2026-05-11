package com.litevar.agent.runner.model;


/**
 * Plugin runtime record.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class PluginRuntime {

    private String pluginId;
    private String containerId;
    private int hostPort;
    private long updatedAt;

    public PluginRuntime() {
    }

    public PluginRuntime(String pluginId, String containerId, int hostPort, long updatedAt) {
        this.pluginId = pluginId;
        this.containerId = containerId;
        this.hostPort = hostPort;
        this.updatedAt = updatedAt;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
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

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
