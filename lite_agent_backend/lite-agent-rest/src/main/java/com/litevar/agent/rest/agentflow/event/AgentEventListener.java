package com.litevar.agent.rest.agentflow.event;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.rest.agentflow.AgentRuntimeFactory;
import com.litevar.agent.rest.agentflow.message.*;
import com.litevar.agent.base.util.SpringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author uncle
 * @since 2025/12/18 16:42
 */
public interface AgentEventListener {
    String ERROR_TOKEN = "context length";

    void onEvent(AgentEvent event);

    default OutMessage transformSendMsg(AgentEvent event) {
        UserSendEvent payload = (UserSendEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("user");
        outMessage.setType("contentList");
        outMessage.setContent(payload.messageList());
        outMessage.setAgentId(event.getAgentId());
        return outMessage;
    }

    default OutMessage transformLlmMsg(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        OutMessage outMessage = llmMessage(event);
        outMessage.setType("text");
        AssistantMessage assistantMessage = msg.response().getChoices().get(0).getMessage();
        outMessage.setContent(assistantMessage.getContent());
        return outMessage;
    }

    default OutMessage transformThinkMsg(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        OutMessage outMessage = llmMessage(event);
        outMessage.setType("think");
        AssistantMessage assistantMessage = msg.response().getChoices().get(0).getMessage();
        outMessage.setContent(assistantMessage.getReasoningContent());
        outMessage.setTokenUsage(null);
        return outMessage;
    }

    default OutMessage transformErrorMsg(AgentEvent event) {
        ErrorEvent msg = (ErrorEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("error");
        Throwable ex = msg.ex();
        String content = "暂时无法连接到服务器，请稍后再试";
        if (ex != null) {
            if (ex instanceof ServiceException) {
                content = ex.getMessage();
            } else if (StrUtil.isNotEmpty(ex.getMessage()) && ex.getMessage().contains(ERROR_TOKEN)) {
                content = "上下文长度超过token限制，请刷新当前界面";
            }
        }
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage transformFunctionCallMsg(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        OutMessage outMessage = llmMessage(event);
        outMessage.setType("functionCallList");
        AssistantMessage assistantMessage = msg.response().getChoices().get(0).getMessage();
        List<OutMessage.FunctionCall> callList = new ArrayList<>();
        assistantMessage.getToolCalls().forEach(i -> {
            OutMessage.FunctionCall functionCall = new OutMessage.FunctionCall();
            functionCall.setId(i.getId());
            functionCall.setName(i.getFunction().getName());
            functionCall.setArguments(i.getFunction().getArguments());
            String functionId = AgentRuntimeFactory.getFunctionId(i.getFunction().getName());
            if (StrUtil.isNotBlank(functionId)) {
                try {
                    ToolFunction function = SpringUtil.getBean(ToolFunctionService.class).findById(functionId);
                    ToolProvider tool = SpringUtil.getBean(ToolService.class).findById(function.getToolId());
                    functionCall.setToolId(function.getToolId());
                    functionCall.setToolName(tool.getName());
                    functionCall.setFunctionName(function.getResource());
                } catch (Exception e) {
                }
            } else {
                //如果大模型给错function,则用他给的function name填回推出去
                functionCall.setFunctionName(i.getFunction().getName());
            }
            functionCall.setFunctionId(functionId);
            callList.add(functionCall);
        });
        outMessage.setToolCalls(callList);
        return outMessage;
    }

    default OutMessage transformToolResultMsg(AgentEvent event) {
        ToolResultEvent msg = (ToolResultEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("tool");
        outMessage.setType("toolReturn");
        outMessage.setContent(msg.result());
        outMessage.setToolCallId(msg.callId());
        return outMessage;
    }

    default OutMessage transformReflectMsg(AgentEvent event) {
        ReflectEvent msg = (ReflectEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("reflection");
        outMessage.setType("reflect");
        OutMessage.ReflectContent content = new OutMessage.ReflectContent();
        content.setRawInput(msg.rawInput());
        content.setRawOutput(msg.rawOutput());
        content.setOutput(msg.reflectOutput());
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage transformDistributeMsg(AgentEvent event) {
        DistributeEvent msg = (DistributeEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getTaskId());
        outMessage.setTaskId(msg.taskId());
        outMessage.setRole("agent");
        outMessage.setType("dispatch");
        OutMessage.DistributeContent content = new OutMessage.DistributeContent();
        content.setTargetAgentId(msg.targetAgentId());
        content.setCmd(msg.cmd());
        content.setImageUrl(msg.imageUrl());
        content.setVideoUrl(msg.videoUrl());
        content.setDispatchId(msg.dispatchId());
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage transformKnowledgeMsg(AgentEvent event) {
        KnowledgeEvent msg = (KnowledgeEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("knowledge");
        OutMessage.KnowledgeContent content = new OutMessage.KnowledgeContent();
        content.setRetrieveContent(msg.retrieveContent());
        content.setInfo(msg.historyInfo());
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage transformAgentSwitchMsg(AgentEvent event) {
        AgentSwitchEvent msg = (AgentSwitchEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.agentId());
        outMessage.setParentTaskId(event.getTaskId());
        outMessage.setTaskId(msg.taskId());
        outMessage.setRole("agent");
        outMessage.setType("agentSwitch");
        OutMessage.AgentSwitchContent content = new OutMessage.AgentSwitchContent();
        content.setAgentName(msg.agentName());
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage transformPlanningMsg(AgentEvent event) {
        PlanningEvent msg = (PlanningEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(event.getAgentId());
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("planning");
        OutMessage.PlanningContent content = new OutMessage.PlanningContent();
        content.setPlanId(msg.planId());
        content.setTaskList(msg.taskList());
        outMessage.setContent(content);
        return outMessage;
    }

    default OutMessage llmMessage(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        OutMessage outMessage = new OutMessage();
        outMessage.setParentTaskId(event.getParentTaskId());
        outMessage.setTaskId(event.getTaskId());
        outMessage.setRole("assistant");
        outMessage.setAgentId(event.getAgentId());
        CompletionResponse response = msg.response();
        outMessage.setId(response.getId());
        if (response.getUsage() != null) {
            outMessage.setTokenUsage(BeanUtil.copyProperties(response.getUsage(), OutMessage.TokenUsage.class));
        }
        return outMessage;
    }
}
