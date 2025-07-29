package com.litevar.agent.base.vo;

import lombok.Data;

import java.util.List;

/**
 * @author uncle
 * @since 2025/7/7 10:11
 */
@Data
public class ApiAgentDetailVO {
    private List<FunctionVO> functionList;
    private List<AgentInfo> subAgentList;

    @Data
    public static class AgentInfo {
        private String id;
        private String name;
    }
}
