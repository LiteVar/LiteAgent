package com.litevar.agent.rest.openai.handler;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.rest.openai.message.LlmMessage;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 获取最后的结果(忽略中间状态数据)
 *
 * @author uncle
 * @since 2025/3/6 16:01
 */
public class ResponseMessageHandler extends AgentMessageHandler {
    @Getter
    private final Map<String, CompletionResponse> responseMap = new HashMap<>();
    @Getter
    private String status = "PROCESSING";
    private final String agentId, requestId, taskId;

    public static final String STATE_COMPLETED = "COMPLETED";

    public ResponseMessageHandler(String agentId, String requestId, String taskId) {
        this.agentId = agentId;
        this.requestId = requestId;
        this.taskId = taskId;
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getRequestId(), this.requestId)) {
            this.responseMap.put(llmMessage.getAgentId(), llmMessage.getResponse());
        }
    }

    @Override
    public void taskDone(String agentId, String taskId) {
        if (StrUtil.equals(this.agentId, agentId) && StrUtil.equals(taskId, this.taskId)) {
            status = STATE_COMPLETED;
        }
    }
}
