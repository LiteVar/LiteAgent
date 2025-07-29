package com.litevar.agent.rest.openai.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.ExternalMessage;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.rest.openai.message.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author uncle
 * @since 2025/3/31 10:33
 */
@Slf4j
public class ExternalApiMessageHandler extends AgentMessageHandler {
    private final FluxSink<ServerSentEvent<String>> sink;
    @Getter
    private final String taskId;
    private final String sessionId;
    private final boolean stream;
    //主agentId
    private final String agentId;

    public ExternalApiMessageHandler(String agentId, String sessionId, String taskId, FluxSink<ServerSentEvent<String>> sink, boolean stream) {
        this.agentId = agentId;
        this.sink = sink;
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.stream = stream;
    }

    @Override
    public void onSend(UserSendMessage userSendMessage) {
        if (StrUtil.equals(userSendMessage.getTaskId(), this.taskId)) {
            sendStatus("start");
            //todo send prompt
            ExternalMessage message = transformSendMsg(userSendMessage);
            sendMessage(message);
        }
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getTaskId(), this.taskId)
                && ObjectUtil.notEqual(llmMessage.getAgentType(), AgentType.REFLECTION.getType())) {
            //不输出反思的结果:{information: xxx,score:1}
            ExternalMessage message = transformLlmMsg(llmMessage);
            message.setType("text");
            if (stream) {
                message.setContent("");
            }
            sendMessage(message);
        }
    }

    @Override
    public void thinkMsg(LlmMessage llmMessage) {
        if (StrUtil.equals(llmMessage.getTaskId(), this.taskId) && !stream) {
            ExternalMessage message = transformLlmMsg(llmMessage);
            message.setType("reasoningContent");
            message.setContent(llmMessage.getResponse().getChoices().get(0).getMessage().getReasoningContent());
            sendMessage(message);
        }
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        if (StrUtil.equals(errorMessage.getTaskId(), this.taskId)) {
            sendStatus("exception");
        }
    }

    @Override
    public void chunk(ChunkMessage chunkMessage) {
        if (StrUtil.equals(chunkMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = new ExternalMessage();
            message.setSessionId(chunkMessage.getSessionId());
            message.setTaskId(chunkMessage.getTaskId());
            message.setRole(isSubAgent(chunkMessage.getAgentId()) ? "subagent" : "assistant");
            message.setTo("agent");
            message.setType(chunkMessage.getChunkType() == 1 ? "reasoningContent" : "text");
            message.setPart(chunkMessage.getPart());

            sendTypeMessage(message, "chunk");
        }
    }

    @Override
    public void functionCalling(LlmMessage functionCallingMessage) {
        if (StrUtil.equals(functionCallingMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformLlmMsg(functionCallingMessage);
            message.setType("toolCalls");
            sendMessage(message);
        }
    }

    @Override
    public void openToolCall(OpenToolMessage openToolMessage) {
        if (StrUtil.equals(openToolMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformOpenToolMsg(openToolMessage);
            log.info("推送open tool 消息给api:{}", message);
            sendTypeMessage(message, "functionCall");
        }
    }

    @Override
    public void functionResult(ToolResultMessage toolResultMessage) {
        if (StrUtil.equals(toolResultMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformFunctionResultMsg(toolResultMessage);
            sendMessage(message);
        }
    }

    @Override
    public void reflect(ReflectResultMessage reflectResultMessage) {
        if (StrUtil.equals(reflectResultMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformReflectMsg(reflectResultMessage);
            sendMessage(message);
        }
    }

    @Override
    public void distribute(DistributeMessage distributeMessage) {
        if (StrUtil.equals(distributeMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformDistributeMsg(distributeMessage);
            sendMessage(message);
        }
    }

    @Override
    public void broadcast(DistributeMessage distributeMessage) {
        distribute(distributeMessage);
    }

    @Override
    public void knowledge(KnowledgeMessage knowledgeMessage) {

    }

    @Override
    public void planning(PlanningMessage planningMessage) {
        if (StrUtil.equals(planningMessage.getTaskId(), this.taskId)) {
            ExternalMessage message = transformPlanningMsg(planningMessage);
            sendMessage(message);
        }
    }

    /**
     * 断开连接
     */
    @Override
    public void disconnect() {
        sendStatus("done");
        try {
            sink.complete();
        } catch (Exception ex) {
            log.error("断开连接失败,sessionId:{},taskId:{}", sessionId, taskId, ex);
            sink.error(ex);
        }
    }

    private void sendStatus(String status) {
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


    private ExternalMessage transformSendMsg(UserSendMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole("user");
        message.setTo("agent");
        message.setType("contentList");
        message.setContent(msg.getMessageList());
        return message;
    }

    private ExternalMessage transformLlmMsg(LlmMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole(isSubAgent(msg.getAgentId()) ? "subagent" : "assistant");
        message.setTo("agent");

        ExternalMessage.Completions completions = new ExternalMessage.Completions();
        CompletionResponse response = msg.getResponse();
        completions.setId(response.getId());
        completions.setModel(response.getModel());
        if (response.getUsage() != null) {
            ExternalMessage.Usage usage = BeanUtil.copyProperties(response.getUsage(), ExternalMessage.Usage.class);
            completions.setUsage(usage);
        }
        message.setCompletions(completions);

        AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
        message.setContent(assistantMessage.getContent());

        if (assistantMessage.hasToolCalls()) {
            List<ExternalMessage.FunctionCall> functionCallList = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                ExternalMessage.FunctionCall functionCall = new ExternalMessage.FunctionCall();
                functionCall.setName(toolCall.getFunction().getName());
                functionCall.setId(toolCall.getId());
                JSONObject argObj = JSONUtil.parseObj(toolCall.getFunction().getArguments());
                functionCall.setArguments(argObj);
                functionCallList.add(functionCall);
            }
            message.setContent(functionCallList);
        }
        return message;
    }

    private ExternalMessage transformOpenToolMsg(OpenToolMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole(isSubAgent(msg.getAgentId()) ? "subagent" : "agent");
        message.setTo("client");
        message.setType("functionCall");
        message.setContent(Dict.create().set("id", msg.getCallId())
                .set("name", msg.getName())
                .set("arguments", msg.getArguments()));
        return message;
    }

    private ExternalMessage transformFunctionResultMsg(ToolResultMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole(isSubAgent(msg.getAgentId()) ? "subagent" : "tool");
        message.setTo("agent");
        message.setType("toolReturn");

        ExternalMessage.ToolReturn toolReturn = new ExternalMessage.ToolReturn();
        toolReturn.setId(msg.getCallId());
        toolReturn.setResult(msg.getResult());
        toolReturn.setFunctionName(msg.getFunctionName());
        message.setContent(toolReturn);
        return message;
    }

    private ExternalMessage transformReflectMsg(ReflectResultMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole(isSubAgent(msg.getAgentId()) ? "subagent" : "reflection");
        message.setTo("agent");
        message.setType("reflection");

        ExternalMessage.ReflectContent content = new ExternalMessage.ReflectContent();
        boolean fail = msg.getReflectOutput().stream().anyMatch(r -> r.getScore() <= 7);
        content.setIsPass(!fail);
        content.setAgentId(msg.getAgentId());
        content.setName(msg.getAgentName());

        ExternalMessage.MessageScore messageScore = new ExternalMessage.MessageScore();
        messageScore.setContent(List.of(Dict.create().set("type", "text").set("message", msg.getRawInput())));
        messageScore.setMessageType("text");
        messageScore.setMessage(msg.getRawOutput());
        messageScore.setReflectScoreList(msg.getReflectOutput());

        content.setMessageScore(messageScore);

        message.setContent(content);
        return message;
    }

    private ExternalMessage transformDistributeMsg(DistributeMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole("subagent");
        message.setTo("agent");
        message.setType("dispatch");
        ExternalMessage.DistributeContent content = new ExternalMessage.DistributeContent();
        content.setDispatchId(msg.getDispatchId());
        content.setAgentId(msg.getTargetAgentId());
        content.setContent(List.of(Dict.create().set("type", "text").set("message", msg.getCmd())));
        message.setContent(content);
        return message;
    }

    private ExternalMessage transformPlanningMsg(PlanningMessage msg) {
        ExternalMessage message = new ExternalMessage();
        message.setSessionId(msg.getSessionId());
        message.setTaskId(msg.getTaskId());
        message.setRole("assistant");
        message.setTo("agent");
        message.setType("planning");
        Dict content = Dict.create().set("planId", msg.getPlanId()).set("plans", ChatService.travelPlanContent(msg.getTaskList()));
        message.setContent(content);
        return message;
    }

    private boolean isSubAgent(String id) {
        return !StrUtil.equals(agentId, id);
    }
}
