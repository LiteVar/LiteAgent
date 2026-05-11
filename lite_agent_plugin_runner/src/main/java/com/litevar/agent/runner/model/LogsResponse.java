package com.litevar.agent.runner.model;


/**
 * Plugin logs response.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
public class LogsResponse {

    private String logs;

    public LogsResponse() {
    }

    public LogsResponse(String logs) {
        this.logs = logs;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }
}
