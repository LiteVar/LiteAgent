package com.litevar.agent.runner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runner configuration properties.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@ConfigurationProperties(prefix = "runner")
public class RunnerProperties {

    private String dataDir = "./data";
    private String uploadDir = "./uploads";
    private String hostDataDir;
    private Security security = new Security();

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getHostDataDir() {
        return hostDataDir;
    }

    public void setHostDataDir(String hostDataDir) {
        this.hostDataDir = hostDataDir;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class Security {
        private long timeWindowSeconds = 10;

        public long getTimeWindowSeconds() {
            return timeWindowSeconds;
        }

        public void setTimeWindowSeconds(long timeWindowSeconds) {
            this.timeWindowSeconds = timeWindowSeconds;
        }
    }

}
