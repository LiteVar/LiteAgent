package com.litevar.agent.base.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.response.ReflectResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 输出前端的message
 *
 * @author uncle
 * @since 2024/9/2 11:58
 */
@Data
public class OutMessage {
    /**
     * 消息属于哪个agent的
     */
    private String agentId;
    private String taskId;
    /**
     * user,assistant,tool,reflection,agent
     */
    private String role;
    /**
     * 消息类型:text,imageUrl,functionCallList,toolReturn,flag,reflect,error,dispatch,agentStatus,knowledge,planning
     */
    private String type;
    private Object content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<FunctionCall> toolCalls;
    private String toolCallId;

    /**
     * 大模型响应的消息id
     */
    private String id;
    private TokenUsage tokenUsage;

    public OutMessage() {
        this.createTime = LocalDateTime.now();
    }

    @Data
    public static class FunctionCall {
        private String id;
        private String name;
        private Object arguments;
    }

    @Data
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    @Data
    public static class KnowledgeContent {
        private String retrieveContent;
        private List<KnowledgeHistoryInfo> info;
    }

    @Data
    public static class KnowledgeHistoryInfo {
        private String id;
        private String datasetName;
        private String datasetId;
    }

    @Data
    public static class ReflectContent {
        @Deprecated
        private String input;

        private String rawInput;
        private String rawOutput;
        private List<ReflectResult> output;
    }

    @Data
    public static class DistributeContent {
        private String cmd;
        private String targetAgentId;
        private String dispatchId;
    }

    @Data
    public static class AgentSwitchContent {
        private String agentName;
    }

    @Data
    public static class PlanningContent {
        private String planId;
        private List<AgentPlanningDTO> taskList;
    }
}
