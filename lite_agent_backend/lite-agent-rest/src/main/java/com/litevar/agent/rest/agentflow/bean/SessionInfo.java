package com.litevar.agent.rest.agentflow.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author uncle
 * @since 2025/12/17 11:00
 */
@Setter
@Getter
public class SessionInfo {
    private String model;
    private String agentId;
    private String userId;
    private String sessionId;
    private Integer debugFlag;
    private Integer callType;
}
