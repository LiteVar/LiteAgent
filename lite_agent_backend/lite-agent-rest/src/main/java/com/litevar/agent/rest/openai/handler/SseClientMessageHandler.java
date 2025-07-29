package com.litevar.agent.rest.openai.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.openai.message.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * WebFlux响应式消息处理器
 *
 * @author uncle
 * @since 2025/7/14
 */
@Slf4j
public class SseClientMessageHandler extends AgentMessageHandler {
    private final FluxSink<ServerSentEvent<String>> sink;
    @Getter
    private final String taskId;
    private final String sessionId;
    private final boolean stream;

    public SseClientMessageHandler(String sessionId, String taskId, FluxSink<ServerSentEvent<String>> sink, boolean stream) {
        this.sink = sink;
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.stream = stream;
    }

    @Override
    public void onSend(UserSendMessage userSendMessage) {
        if (StrUtil.equals(userSendMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformSendMessage(userSendMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getTaskId(), this.taskId) && !stream
                && ObjectUtil.notEqual(llmMessage.getAgentType(), AgentType.REFLECTION.getType())) {
            OutMessage outMessage = transformLlmMessage(llmMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void thinkMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getTaskId(), this.taskId) && !stream) {
            OutMessage outMessage = transformThinkMessage(llmMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        if (StrUtil.equals(errorMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformErrorMessage(errorMessage);
            sendTypeMessage(outMessage, "error");
        }
    }

    @Override
    public void chunk(ChunkMessage chunkMessage) {
        if (StrUtil.equals(chunkMessage.getTaskId(), this.taskId)) {
            JSONObject p = new JSONObject().set("part", chunkMessage.getPart())
                    .set("taskId", taskId)
                    .set("agentId", chunkMessage.getAgentId())
                    .set("chunkType", chunkMessage.getChunkType());
            sendTypeMessage(p, "delta");
        }
    }

    @Override
    public void functionCalling(LlmMessage functionCallingMessage) {
        if (StrUtil.equals(functionCallingMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformFunctionCallMessage(functionCallingMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void functionResult(ToolResultMessage toolResultMessage) {
        if (StrUtil.equals(toolResultMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformToolResultMessage(toolResultMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void reflect(ReflectResultMessage reflectResultMessage) {
        if (StrUtil.equals(reflectResultMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformReflectMessage(reflectResultMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void distribute(DistributeMessage distributeMessage) {
        if (StrUtil.equals(distributeMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformDistributeMessage(distributeMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void broadcast(DistributeMessage distributeMessage) {
        distribute(distributeMessage);
    }

    @Override
    public void knowledge(KnowledgeMessage knowledgeMessage) {
        if (StrUtil.equals(knowledgeMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformKnowledgeMessage(knowledgeMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void agentSwitch(AgentSwitchMessage agentSwitchMessage) {
        if (StrUtil.equals(agentSwitchMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformAgentStatusMessage(agentSwitchMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void planning(PlanningMessage planningMessage) {
        if (StrUtil.equals(planningMessage.getTaskId(), this.taskId)) {
            OutMessage outMessage = transformPlanningMessage(planningMessage);
            sendMessage(outMessage);
        }
    }

    private void sendMessage(OutMessage outMessage) {
        sendTypeMessage(outMessage, "message");
    }

    private void sendTypeMessage(Object msg, String type) {
        if (sink.isCancelled()) {
            log.warn("Sink已取消,跳过消息发送,sessionId:{},taskId:{}", sessionId, taskId);
            return;
        }
        
        try {
            String data = JSONUtil.toJsonStr(msg);
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(type)
                    .data(data)
                    .build();
            sink.next(event);
        } catch (Exception ex) {
            log.error("发送消息失败,sessionId:{},taskId:{}", sessionId, taskId, ex);
            sink.error(ex);
        }
    }

    @Override
    public void disconnect() {
        try {
            sendTypeMessage("[DONE]", "end");
            sink.complete();
        } catch (Exception ex) {
            log.error("断开连接失败,sessionId:{},taskId:{}", sessionId, taskId, ex);
            sink.error(ex);
        }
    }
}