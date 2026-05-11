package com.litevar.agent.base.vo;

import lombok.Data;

/**
 * @author uncle
 * @since 2026/2/4 12:23
 */
@Data
public class PluginAgentVO {
    /**
     * agent id
     */
    private String id;
    /**
     * agent name
     */
    private String name;

    /**
     * agent api url
     */
    private String apiUrl;

    /**
     * agent api key
     */
    private String apiKey;
}
