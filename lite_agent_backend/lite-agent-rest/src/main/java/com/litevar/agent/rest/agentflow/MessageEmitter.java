package com.litevar.agent.rest.agentflow;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.response.ReflectResult;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventBus;
import com.litevar.agent.rest.agentflow.event.AgentEventType;
import com.litevar.agent.rest.agentflow.message.*;
import jakarta.annotation.Resource;

import java.util.List;

/**
 * @author uncle
 * @since 2026/3/9 15:02
 */
public abstract class MessageEmitter {

    @Resource
    private AgentEventBus bus;

    protected void thinkMessage(AgentContext context, CompletionResponse response) {
        llmMessageInternal(context, response, AgentEventType.THINK_EVENT);
    }

    protected void textMessage(AgentContext context, CompletionResponse response) {
        llmMessageInternal(context, response, AgentEventType.LLM_EVENT);
    }

    protected void functionCallMessage(AgentContext context, CompletionResponse response) {
        llmMessageInternal(context, response, AgentEventType.FUNCTION_CALL_EVENT);
    }

    private void llmMessageInternal(AgentContext context, CompletionResponse response, AgentEventType eventType) {
        Integer agentType = context.getRuntimeInfo().getAgentType();
        bus.publish(new AgentEvent(context, new LlmEvent(agentType, response, eventType)));
    }

    protected void chunkMessage(AgentContext context, String chunk, int chunkType) {
        bus.publish(new AgentEvent(context, new ChunkEvent(chunkType, chunk)));
    }

    protected void errorMessage(AgentContext context, Throwable error) {
        bus.publish(new AgentEvent(context, new ErrorEvent(error)));
    }

    public void agentSwitchMessage(AgentContext context, String taskId, String agentId, String agentName) {
        bus.publish(new AgentEvent(context, new AgentSwitchEvent(taskId, agentId, agentName)));
    }

    public void agentDistributeMessage(AgentContext context, String taskId, String cmd, List<String> imageUrls,
                                          String videoUrl, String targetAgentId) {
        bus.publish(new AgentEvent(context, new DistributeEvent(taskId, cmd, imageUrls, videoUrl, targetAgentId, IdUtil.getSnowflakeNextIdStr())));
    }

    public void reflectMessage(AgentContext context, String inputMsg, String outputMsg, List<ReflectResult> reflectResultList) {
        bus.publish(new AgentEvent(context, new ReflectEvent(inputMsg, outputMsg, reflectResultList)));
    }

    protected void knowledgeBaseMessage(AgentContext context, String content, List<OutMessage.KnowledgeHistoryInfo> history) {
        bus.publish(new AgentEvent(context, new KnowledgeEvent(content, history)));
    }

    protected void thirdOpenToolMessage(AgentContext context, String callId, String functionName, JSONObject args) {
        bus.publish(new AgentEvent(context, new OpenToolEvent(callId, functionName, args)));
    }

    protected void toolResultMessage(AgentContext context, String callId, String result, String functionId) {
        bus.publish(new AgentEvent(context, new ToolResultEvent(callId, result, functionId)));
    }

    public void planMessage(AgentContext context, String planId, List<AgentPlanningDTO> taskList) {
        bus.publish(new AgentEvent(context, new PlanningEvent(taskList, planId)));
    }
}
