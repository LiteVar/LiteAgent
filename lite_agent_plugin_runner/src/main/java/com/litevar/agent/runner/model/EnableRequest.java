package com.litevar.agent.runner.model;

/**
 * Enable plugin request.
 *
 * @author uncle
 * @since 2026/01/16 10:00
 */
public class EnableRequest {

    private String packageUrl;

    public String getPackageUrl() {
        return packageUrl;
    }

    public void setPackageUrl(String packageUrl) {
        this.packageUrl = packageUrl;
    }
}
