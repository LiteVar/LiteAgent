package com.litevar.agent.rest.agentflow.listener;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.agentflow.message.ChunkEvent;
import com.litevar.agent.rest.agentflow.message.LlmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * web客户端消息监听器
 *
 * @author uncle
 * @since 2025/12/19 10:53
 */
@Slf4j
public record WebClientEventListener(FluxSink<ServerSentEvent<String>> sink, String requestId,
                                     boolean stream) implements AgentEventListener, DisconnectableEventListener {

    @Override
    public void onEvent(AgentEvent event) {
        if (!StrUtil.equals(event.getRequestId(), requestId)) {
            return;
        }
        if (sink.isCancelled()) {
            log.warn("Sink已取消,跳过消息发送,sessionId:{},requestId:{}", event.getSessionId(), event.getRequestId());
            return;
        }
        switch (event.getPayload().type()) {
            case USER_SEND_EVENT -> userSendMsg(event);
            case LLM_EVENT -> llmMsg(event);
            case THINK_EVENT -> thinkMsg(event);
            case ERROR_EVENT -> errorMsg(event);
            case CHUNK_EVENT -> chunkMsg(event);
            case FUNCTION_CALL_EVENT -> functionCallMsg(event);
            case TOOL_RESULT_EVENT -> functionResultMsg(event);
            case REFLECTION_EVENT -> reflectMsg(event);
            case AGENT_DISPATCH_EVENT -> distributeMsg(event);
            case KNOWLEDGE_EVENT -> knowledgeMsg(event);
            case AGENT_SWITCH_EVENT -> switchAgentMsg(event);
            case PLANNING_EVENT -> planningMsg(event);
            default -> {
            }
        }
    }

    private void userSendMsg(AgentEvent event) {
        OutMessage outMessage = transformSendMsg(event);
        sendMessage(outMessage);
    }

    private void llmMsg(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        if (!stream && ObjectUtil.notEqual(msg.agentType(), AgentType.REFLECTION.getType())) {
            OutMessage outMessage = transformLlmMsg(event);
            sendMessage(outMessage);
        }
    }

    private void thinkMsg(AgentEvent event) {
        if (!stream) {
            OutMessage outMessage = transformThinkMsg(event);
            sendMessage(outMessage);
        }
    }

    private void errorMsg(AgentEvent event) {
        OutMessage outMessage = transformErrorMsg(event);
        sendTypeMessage(outMessage, "error");
    }

    private void chunkMsg(AgentEvent event) {
        ChunkEvent msg = (ChunkEvent) event.getPayload();
        JSONObject p = new JSONObject().set("part", msg.part())
                .set("taskId", event.getTaskId())
                .set("parentTaskId", event.getParentTaskId())
                .set("agentId", event.getAgentId())
                .set("chunkType", msg.chunkType());
        sendTypeMessage(p, "delta");
    }

    private void functionCallMsg(AgentEvent event) {
        OutMessage outMessage = transformFunctionCallMsg(event);
        sendMessage(outMessage);
    }

    private void functionResultMsg(AgentEvent event) {
        OutMessage outMessage = transformToolResultMsg(event);
        sendMessage(outMessage);
    }

    private void reflectMsg(AgentEvent event) {
        OutMessage outMessage = transformReflectMsg(event);
        sendMessage(outMessage);
    }

    private void distributeMsg(AgentEvent event) {
        OutMessage outMessage = transformDistributeMsg(event);
        sendMessage(outMessage);
    }

    private void knowledgeMsg(AgentEvent event) {
        OutMessage outMessage = transformKnowledgeMsg(event);
        sendMessage(outMessage);
    }

    private void switchAgentMsg(AgentEvent event) {
        OutMessage outMessage = transformAgentSwitchMsg(event);
        sendMessage(outMessage);
    }

    private void planningMsg(AgentEvent event) {
        OutMessage outMessage = transformPlanningMsg(event);
        sendMessage(outMessage);
    }

    private void sendMessage(OutMessage outMessage) {
        sendTypeMessage(outMessage, "message");
    }

    private void sendTypeMessage(Object msg, String type) {
        try {
            String data = JSONUtil.toJsonStr(msg);
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(type)
                    .data(data)
                    .build();
            sink.next(event);
        } catch (Exception ex) {
            log.error("发送消息失败", ex);
            sink.error(ex);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (!sink.isCancelled()) {
                sendTypeMessage("[DONE]", "end");
                sink.complete();
            }
        } catch (Exception ex) {
            log.error("断开连接失败:{}", requestId, ex);
            if (!sink.isCancelled()) {
                sink.error(ex);
            }
        }
    }
}
