package com.litevar.agent.rest.agentflow.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.ExternalMessage;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.rest.agentflow.AgentRuntimeFactory;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.agentflow.message.*;
import com.litevar.agent.base.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

/**
 * agent对外API
 *
 * @author uncle
 * @since 2025/12/19 14:28
 */
@Slf4j
public record ExternalApiEventListener(FluxSink<ServerSentEvent<String>> sink, String sessionId, String requestId,
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
            case OPEN_TOOL_CALL_EVENT -> openToolCallMsg(event);
            case TOOL_RESULT_EVENT -> functionResultMsg(event);
            case REFLECTION_EVENT -> reflectMsg(event);
            case AGENT_DISPATCH_EVENT -> distributeMsg(event);
            case PLANNING_EVENT -> planningMsg(event);
            default -> {
            }
        }
    }

    private void userSendMsg(AgentEvent event) {
        sendStatus(event.getSessionId(), event.getTaskId(), "start");
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(null);
        message.setTaskId(event.getTaskId());
        message.setRole("user");
        message.setTo("agent");
        message.setType("contentList");
        UserSendEvent payload = (UserSendEvent) event.getPayload();
        message.setContent(payload.messageList());
        sendMessage(message);
    }

    private void llmMsg(AgentEvent event) {
        LlmEvent payload = (LlmEvent) event.getPayload();
        if (ObjectUtil.notEqual(payload.agentType(), AgentType.REFLECTION.getType())) {
            ExternalMessage message = transformLlmMessage(event);
            message.setType("text");
            if (stream) {
                message.setContent("");
            }
            sendMessage(message);
        }
    }

    private void thinkMsg(AgentEvent event) {
        if (stream) {
            return;
        }
        ExternalMessage message = transformLlmMessage(event);
        LlmEvent payload = (LlmEvent) event.getPayload();
        message.setType("reasoningContent");
        message.setContent(payload.response().getChoices().get(0).getMessage().getReasoningContent());
        sendMessage(message);
    }

    private void errorMsg(AgentEvent event) {
        sendStatus(event.getSessionId(), event.getTaskId(), "exception");
    }

    private void chunkMsg(AgentEvent event) {
        ChunkEvent payload = (ChunkEvent) event.getPayload();
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setTaskId(event.getTaskId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setRole(isSubAgent(event) ? "subagent" : "assistant");
        message.setTo("agent");
        message.setType(payload.chunkType() == 1 ? "reasoningContent" : "text");
        message.setPart(payload.part());
        sendTypeMessage(message, "chunk");
    }

    private void functionCallMsg(AgentEvent event) {
        LlmEvent payload = (LlmEvent) event.getPayload();
        ExternalMessage message = transformLlmMessage(event);
        AssistantMessage assistantMessage = payload.response().getChoices().get(0).getMessage();
        if (assistantMessage.hasToolCalls()) {
            List<ExternalMessage.FunctionCall> functionCallList = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                ExternalMessage.FunctionCall functionCall = new ExternalMessage.FunctionCall();
                functionCall.setName(toolCall.getFunction().getName());
                functionCall.setId(toolCall.getId());
                JSONObject argObj = JSONUtil.parseObj(toolCall.getFunction().getArguments());
                functionCall.setArguments(argObj);
                try {
                    String functionId = AgentRuntimeFactory.getFunctionId(toolCall.getFunction().getName());
                    ToolFunction function = SpringUtil.getBean(ToolFunctionService.class).findById(functionId);
                    ToolProvider tool = SpringUtil.getBean(ToolService.class).findById(function.getToolId());
                    functionCall.setToolId(function.getToolId());
                    functionCall.setToolName(tool.getName());
                    functionCall.setFunctionId(functionId);
                    functionCall.setFunctionName(function.getResource());
                } catch (Exception e) {
                }
                functionCallList.add(functionCall);
            }
            message.setContent(functionCallList);
        }
        message.setType("toolCalls");
        sendMessage(message);
    }

    private void openToolCallMsg(AgentEvent event) {
        OpenToolEvent payload = (OpenToolEvent) event.getPayload();
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setTaskId(event.getTaskId());
        message.setRole(isSubAgent(event) ? "subagent" : "agent");
        message.setTo("client");
        message.setType("functionCall");
        message.setContent(Dict.create().set("id", payload.callId())
                .set("name", payload.name())
                .set("arguments", payload.arguments()));
        log.info("推送open tool 消息给api:{}", message);
        sendTypeMessage(message, "functionCall");
    }

    private void functionResultMsg(AgentEvent event) {
        ToolResultEvent payload = (ToolResultEvent) event.getPayload();
        ExternalMessage message = transformFunctionResultMsg(event, payload);
        sendMessage(message);
    }

    private void reflectMsg(AgentEvent event) {
        ReflectEvent payload = (ReflectEvent) event.getPayload();
        ExternalMessage message = transformReflectMsg(event, payload);
        sendMessage(message);
    }

    private void distributeMsg(AgentEvent event) {
        DistributeEvent payload = (DistributeEvent) event.getPayload();
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getTaskId());
        message.setTaskId(payload.taskId());
        message.setRole("subagent");
        message.setTo("agent");
        message.setType("dispatch");
        ExternalMessage.DistributeContent content = new ExternalMessage.DistributeContent();
        content.setDispatchId(payload.dispatchId());
        content.setAgentId(payload.targetAgentId());
        List<Dict> contentList = new ArrayList<>();
        Dict dict = Dict.create().set("type", "text").set("message", payload.cmd());
        contentList.add(dict);
        if (!payload.imageUrl().isEmpty()) {
            payload.imageUrl().forEach(url -> contentList.add(Dict.create().set("type", "imageUrl").set("message", url)));
        }
        if (StrUtil.isNotEmpty(payload.videoUrl())) {
            contentList.add(Dict.create().set("type", "videoUrl").set("message", payload.videoUrl()));
        }
        content.setContent(contentList);
        message.setContent(content);
        sendMessage(message);
    }

    private void planningMsg(AgentEvent event) {
        PlanningEvent payload = (PlanningEvent) event.getPayload();
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setTaskId(event.getTaskId());
        message.setRole("assistant");
        message.setTo("agent");
        message.setType("planning");
        Dict content = Dict.create().set("planId", payload.planId()).set("plans", ChatService.travelPlanContent(payload.taskList()));
        message.setContent(content);
        sendMessage(message);
    }

    private ExternalMessage transformLlmMessage(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setTaskId(event.getTaskId());
        message.setRole(isSubAgent(event) ? "subagent" : "assistant");
        message.setTo("agent");

        ExternalMessage.Completions completions = new ExternalMessage.Completions();
        CompletionResponse response = msg.response();
        completions.setId(response.getId());
        completions.setModel(response.getModel());
        if (response.getUsage() != null) {
            ExternalMessage.Usage usage = BeanUtil.copyProperties(response.getUsage(), ExternalMessage.Usage.class);
            completions.setUsage(usage);
        }
        message.setCompletions(completions);

        AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
        message.setContent(assistantMessage.getContent());
        return message;
    }

    private ExternalMessage transformFunctionResultMsg(AgentEvent event, ToolResultEvent msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setTaskId(event.getTaskId());
        message.setRole(isSubAgent(event) ? "subagent" : "tool");
        message.setTo("agent");
        message.setType("toolReturn");

        ExternalMessage.ToolReturn toolReturn = new ExternalMessage.ToolReturn();
        try {
            ToolFunction function = SpringUtil.getBean(ToolFunctionService.class).findById(msg.functionId());
            toolReturn.setFunctionName(function.getResource());
            toolReturn.setFunctionId(function.getId());
            toolReturn.setToolId(function.getToolId());
            ToolProvider tool = SpringUtil.getBean(ToolService.class).findById(function.getToolId());
            toolReturn.setToolName(tool.getName());
        } catch (Exception ex) {
        }
        toolReturn.setId(msg.callId());
        toolReturn.setResult(msg.result());
        message.setContent(toolReturn);
        return message;
    }

    private ExternalMessage transformReflectMsg(AgentEvent event, ReflectEvent msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(event.getSessionId());
        message.setAgentId(event.getAgentId());
        message.setParentTaskId(event.getParentTaskId());
        message.setTaskId(event.getTaskId());
        message.setRole(isSubAgent(event) ? "subagent" : "reflection");
        message.setTo("agent");
        message.setType("reflection");

        ExternalMessage.ReflectContent content = new ExternalMessage.ReflectContent();
        boolean fail = msg.reflectOutput().stream().anyMatch(r -> r.getScore() <= 7);
        content.setIsPass(!fail);
        content.setAgentId(event.getAgentId());
        content.setName(null);

        ExternalMessage.MessageScore messageScore = new ExternalMessage.MessageScore();
        messageScore.setContent(List.of(Dict.create().set("type", "text").set("message", msg.rawInput())));
        messageScore.setMessageType("text");
        messageScore.setMessage(msg.rawOutput());
        messageScore.setReflectScoreList(msg.reflectOutput());

        content.setMessageScore(messageScore);

        message.setContent(content);
        return message;
    }

    private boolean isSubAgent(AgentEvent event) {
        return StrUtil.isNotEmpty(event.getParentTaskId());
    }

    private void sendStatus(String sessionId, String taskId, String status) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(sessionId);
        message.setTaskId(taskId);
        message.setRole("agent");
        message.setTo("client");
        message.setType("taskStatus");
        message.setContent(Dict.create().set("status", status));
        sendTypeMessage(message, "message");
    }

    private void sendMessage(ExternalMessage message) {
        log.info("推送消息给api:{}", message);
        sendTypeMessage(message, "message");
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
                sendStatus(sessionId, requestId, "done");
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
