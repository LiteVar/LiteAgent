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
    private final String sessionId;
    private final boolean stream;
    @Getter
    private final String requestId;

    public SseClientMessageHandler(String sessionId, String requestId, FluxSink<ServerSentEvent<String>> sink, boolean stream) {
        this.sink = sink;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.stream = stream;
    }

    @Override
    public void onSend(UserSendMessage userSendMessage) {
        if (StrUtil.equals(userSendMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformSendMessage(userSendMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getRequestId(), this.requestId) && !stream
                && ObjectUtil.notEqual(llmMessage.getAgentType(), AgentType.REFLECTION.getType())) {
            OutMessage outMessage = transformLlmMessage(llmMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void thinkMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getRequestId(), this.requestId) && !stream) {
            OutMessage outMessage = transformThinkMessage(llmMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        if (StrUtil.equals(errorMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformErrorMessage(errorMessage);
            sendTypeMessage(outMessage, "error");
        }
    }

    @Override
    public void chunk(ChunkMessage chunkMessage) {
        if (StrUtil.equals(chunkMessage.getRequestId(), this.requestId)) {
            JSONObject p = new JSONObject().set("part", chunkMessage.getPart())
                    .set("taskId", chunkMessage.getTaskId())
                    .set("parentTaskId", chunkMessage.getParentTaskId())
                    .set("agentId", chunkMessage.getAgentId())
                    .set("chunkType", chunkMessage.getChunkType());
            sendTypeMessage(p, "delta");
        }
    }

    @Override
    public void functionCalling(LlmMessage functionCallingMessage) {
        if (StrUtil.equals(functionCallingMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformFunctionCallMessage(functionCallingMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void functionResult(ToolResultMessage toolResultMessage) {
        if (StrUtil.equals(toolResultMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformToolResultMessage(toolResultMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void reflect(ReflectResultMessage reflectResultMessage) {
        if (StrUtil.equals(reflectResultMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformReflectMessage(reflectResultMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void distribute(DistributeMessage distributeMessage) {
        if (StrUtil.equals(distributeMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformDistributeMessage(distributeMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void knowledge(KnowledgeMessage knowledgeMessage) {
        if (StrUtil.equals(knowledgeMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformKnowledgeMessage(knowledgeMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void agentSwitch(AgentSwitchMessage agentSwitchMessage) {
        if (StrUtil.equals(agentSwitchMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformAgentSwitchMessage(agentSwitchMessage);
            sendMessage(outMessage);
        }
    }

    @Override
    public void planning(PlanningMessage planningMessage) {
        if (StrUtil.equals(planningMessage.getRequestId(), this.requestId)) {
            OutMessage outMessage = transformPlanningMessage(planningMessage);
            sendMessage(outMessage);
        }
    }

    private void sendMessage(OutMessage outMessage) {
        sendTypeMessage(outMessage, "message");
    }

    private void sendTypeMessage(Object msg, String type) {
        if (sink.isCancelled()) {
            log.warn("Sink已取消,跳过消息发送,sessionId:{},requestId:{}", sessionId, requestId);
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
            log.error("发送消息失败,sessionId:{},requestId:{}", sessionId, requestId, ex);
            sink.error(ex);
        }
    }

    @Override
    public void disconnect(String requestId) {
        try {
            if (!sink.isCancelled()) {
                sendTypeMessage("[DONE]", "end");
                sink.complete();
            }
        } catch (Exception ex) {
            log.error("断开连接失败,sessionId:{},requestId:{}", sessionId, requestId, ex);
            if (!sink.isCancelled()) {
                sink.error(ex);
            }
        }
    }
}