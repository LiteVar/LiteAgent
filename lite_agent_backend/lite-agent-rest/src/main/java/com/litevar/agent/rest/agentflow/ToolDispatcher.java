package com.litevar.agent.rest.agentflow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.base.vo.SegmentVO;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.agent.core.module.tool.executor.OpenToolThirdExecutor;
import com.litevar.agent.openai.completion.ChatModelRequest;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.ToolMessage;
import com.litevar.agent.rest.agentflow.agent.DistributeAgent;
import com.litevar.agent.rest.agentflow.agent.Orchestrator;
import com.litevar.agent.rest.agentflow.agent.PlanningAgent;
import com.litevar.agent.rest.agentflow.auto.execution.PlanExecutionScheduler;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.executor.TaskExecutor;
import com.litevar.agent.rest.service.SegmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * tool分发调用
 *
 * @author uncle
 * @since 2026/3/9 14:59
 */
@Slf4j
@Component
public class ToolDispatcher {
    @Resource
    private ExecutionStopManager stopManager;
    @Resource
    private SegmentService segmentService;
    @Lazy
    @Resource
    private Orchestrator orchestrator;
    @Resource
    private ToolService toolService;
    @Resource
    private ToolFunctionService toolFunctionService;
    @Resource(name = "asyncTaskExecutor")
    private Executor asyncTaskExecutor;
    @Resource
    private PlanningAgent planningAgent;
    @Resource
    private PlanExecutionScheduler planExecutionScheduler;
    @Resource
    private DistributeAgent distributeAgent;

    public List<Message> dispatch(AgentContext context, CompletionResponse response) {
        if (stopManager.shouldStop(context.getRequestId())) {
            return Collections.emptyList();
        }
        List<AssistantMessage.ToolCall> toolCalls = response.getChoices().get(0).getMessage().getToolCalls();
        List<FunctionCallInfo> functionCallList = toolCalls.stream().map(
                i -> new FunctionCallInfo(i.getId(), i.getFunction().getName(), i.getFunction().getArguments())).toList();

        // 发function calling 消息
        CompletionResponse clone = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
        clone.getChoices().get(0).getMessage().getToolCalls().removeIf(i -> StrUtil.equalsAny(i.getFunction().getName(),
                AgentRuntimeFactory.TOOL_KNOWLEDGE_BASE, AgentRuntimeFactory.TOOL_AGENT_DISTRIBUTE,
                AgentRuntimeFactory.TOOL_AUTO_AGENT, AgentRuntimeFactory.TOOL_PLAN_DISPATCH));
        if (clone.getChoices().get(0).getMessage().hasToolCalls()) {
            orchestrator.functionCallMessage(context, clone);
        }

        List<Message> resultList = new ArrayList<>();
        for (FunctionCallInfo functionCallInfo : functionCallList) {
            if (stopManager.shouldStop(context.getRequestId())) {
                return Collections.emptyList();
            }
            String result = null;
            if (StrUtil.equals(functionCallInfo.functionName, AgentRuntimeFactory.TOOL_KNOWLEDGE_BASE)) {
                //知识库
                result = knowledgeBase(context, functionCallInfo);

            } else if (StrUtil.equals(functionCallInfo.functionName, AgentRuntimeFactory.TOOL_AGENT_DISTRIBUTE)) {
                //子agent调度
                result = distributeAgent.distribute(context, functionCallInfo);

            } else if (StrUtil.equals(functionCallInfo.functionName, AgentRuntimeFactory.TOOL_AUTO_AGENT)) {
                //planning agent
                result = planningAgent.planning(context,
                        functionCallInfo.callId, JSONUtil.parseObj(functionCallInfo.argument).getStr("task"));
                if (result != null) {
                    log.error("【planning result】sessionId={},requestId={},{}", context.getSessionId(), context.getRequestId(), result);
                }

            } else if (StrUtil.equals(functionCallInfo.functionName, AgentRuntimeFactory.TOOL_PLAN_DISPATCH)) {
                //plan agent dispatch
                log.info("【plan agent dispatch】sessionId={},requestId={},{}", context.getSessionId(), context.getRequestId(), functionCallInfo.argument);
                result = planExecutionScheduler.dispatch(context, JSONUtil.parseObj(functionCallInfo.argument));
                log.info("【plan agent result】sessionId={},requestId={},{}", context.getSessionId(), context.getRequestId(), result);
            } else {
                //tool
                result = executeTool(context, functionCallInfo);
            }
            if (StrUtil.isNotEmpty(result)) {
                ToolMessage toolMessage = new ToolMessage();
                toolMessage.setToolCallId(functionCallInfo.callId);
                toolMessage.setContent(result);
                resultList.add(toolMessage);
            } else {
                response.getChoices().get(0).getMessage().getToolCalls().removeIf(i -> i.getId().equals(functionCallInfo.callId));
            }
        }

        if (!resultList.isEmpty()) {
            resultList.add(0, response.getChoices().get(0).getMessage());
        }

        return resultList;
    }

    /**
     * 知识库检索
     */
    private String knowledgeBase(AgentContext context, FunctionCallInfo functionCallInfo) {
        String content = JSONUtil.parseObj(functionCallInfo.argument).getStr("content");
        String result;
        List<OutMessage.KnowledgeHistoryInfo> history = Collections.emptyList();
        try {
            log.info("[sessionId={},taskId={}]知识库检索内容:{}", context.getSessionId(), context.getTaskId(), content);
            Dict dict = segmentService.retrieve(context.getAgentId(), context.getRuntimeInfo().getDatasetIds(), content, context.getUserId());
            @SuppressWarnings("unchecked")
            List<SegmentVO> segments = (List<SegmentVO>) dict.get("result");
            if (segments == null || segments.isEmpty()) {
                result = "knowledge base retrieve results is empty";
            } else {
                history = (List<OutMessage.KnowledgeHistoryInfo>) dict.get("history");
                result = segments.stream().map(SegmentVO::getContent).collect(Collectors.joining("\n"));
            }
        } catch (ServiceException ex) {
            log.error("[sessionId={},taskId={}]knowledge base retrieve failed", context.getSessionId(), context.getTaskId(), ex);
            result = ex.getMessage();
        } catch (Exception ex) {
            log.error("[sessionId={},taskId={}]knowledge base retrieve failed", context.getSessionId(), context.getTaskId(), ex);
            result = "knowledge base retrieve failed";
        }
        log.info("[sessionId={},taskId={},知识库检索结果:{}", context.getSessionId(), context.getTaskId(), result);
        orchestrator.knowledgeBaseMessage(context, content, history);
        return result;
    }

    private String executeTool(AgentContext context, FunctionCallInfo functionCallInfo) {
        String functionName = functionCallInfo.functionName();
        String functionId = AgentRuntimeFactory.getFunctionId(functionName);
        final ToolFunction function;
        String callId = functionCallInfo.callId;
        if (StrUtil.isEmpty(functionId) || (function = toolFunctionService.findById(functionId)) == null) {
            String result = "function does not exist:" + functionName;
            orchestrator.toolResultMessage(context, callId, result, functionId);
            return result;
        }
        JSONObject args = JSONUtil.parseObj(functionCallInfo.argument);
        String arguments = functionCallInfo.argument;
        String apiKey = toolService.toolApiKey(function.getToolId());
        Integer executeMode = context.getRuntimeInfo().getFunctionExecuteMode().get(function.getId());
        FunctionExecutor executor = ToolHandleFactory.getFunctionExecutor(function.getProtocol());
        Map<String, String> defineHeader = new HashMap<>();
        if (StrUtil.isNotEmpty(apiKey)) {
            defineHeader.put(HttpHeaders.AUTHORIZATION, apiKey);
        }

        stopManager.markToolStart(context.getRequestId());
        Callable<Object> task = () -> {
            if (executor instanceof OpenToolThirdExecutor) {
                orchestrator.thirdOpenToolMessage(context, callId, function.getResource(), args);
            }
            try {
                if (StrUtil.equals(function.getContentType(), MediaType.TEXT_EVENT_STREAM_VALUE)) {
                    AssistantMessage.ToolCall toolCall = buildToolCall(functionName, arguments, callId);
                    StreamConsumer streamConsumer = new StreamConsumer(context, cloneContext(context), toolCall, functionId);
                    try {
                        executor.streamCall(callId, function, args, streamConsumer::offer);
                    } catch (Exception ex) {
                        streamConsumer.offer(List.of(FunctionExecutor.STREAM_DONE));
                        throw ex;
                    }
                    streamConsumer.await();
                    return null;
                }
                return executor.invoke(callId, function, args, defineHeader);
            } catch (Exception ex) {
                log.error("Fail to invoke the tool", ex);
                return "Fail to invoke the tool.";
            }
        };

        try {
            CompletableFuture<Object> future = TaskExecutor.execute(context.getAgentId() + function.getId(), executeMode, task);
            String result = (String) future.join();
            if (StrUtil.isNotEmpty(result)) {
                orchestrator.toolResultMessage(context, callId, result, functionId);
            }
            return result;
        } catch (Exception ex) {
            return ex.getMessage();
        } finally {
            stopManager.markToolEnd(context.getRequestId());
        }
    }

    private AssistantMessage.ToolCall buildToolCall(String functionName, String arguments, String callId) {
        AssistantMessage.Function function = new AssistantMessage.Function();
        function.setName(functionName);
        function.setArguments(arguments);
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall();
        toolCall.setId(callId);
        toolCall.setType("function");
        toolCall.setFunction(function);
        return toolCall;
    }

    private class StreamConsumer {
        private final AgentContext context;
        private final AgentContext cloneContext;
        private final AssistantMessage.ToolCall toolCall;
        private final String functionId;
        private final Queue<String> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean draining = new AtomicBoolean(false);
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean doneReceived = new AtomicBoolean(false);
        private final AtomicBoolean modelRunning = new AtomicBoolean(false);
        private final CompletableFuture<Void> consumerDone = new CompletableFuture<>();

        private StreamConsumer(AgentContext context, AgentContext cloneContext, AssistantMessage.ToolCall toolCall, String functionId) {
            this.context = context;
            this.cloneContext = cloneContext;
            this.toolCall = toolCall;
            this.functionId = functionId;
        }

        void offer(List<String> list) {
            queue.addAll(list);
            schedule();
        }

        void await() {
            consumerDone.join();
        }

        private void schedule() {
            if (consumerDone.isDone()) {
                return;
            }
            if (draining.compareAndSet(false, true)) {
                asyncTaskExecutor.execute(this::drain);
            }
        }

        private void drain() {
            try {
                while (true) {
                    List<String> list = new ArrayList<>();
                    boolean done = false;
                    String single;
                    while ((single = queue.poll()) != null) {
                        if (StrUtil.equals(single, FunctionExecutor.STREAM_DONE)) {
                            done = true;
                            continue;
                        }
                        list.add(single);
                    }
                    if (done) {
                        doneReceived.set(true);
                    }
                    if (list.isEmpty()) {
                        if (doneReceived.get() && !modelRunning.get()) {
                            consumerDone.complete(null);
                        }
                        return;
                    }

                    List<Message> submitMsg = new ArrayList<>();
                    String streamCallId = IdUtil.getSnowflakeNextIdStr();
                    AssistantMessage.ToolCall streamToolCall = new AssistantMessage.ToolCall();
                    streamToolCall.setId(streamCallId);
                    streamToolCall.setType(toolCall.getType());
                    streamToolCall.setFunction(toolCall.getFunction());
                    AssistantMessage assistantMessage = new AssistantMessage();
                    assistantMessage.setToolCalls(List.of(streamToolCall));
                    submitMsg.add(assistantMessage);

                    ToolMessage toolMessage = new ToolMessage();
                    toolMessage.setToolCallId(streamCallId);
                    submitMsg.add(toolMessage);

                    if (started.compareAndSet(false, true)) {
                        String startTip = "The tool was successfully invoked and is currently processing,please wait!";
                        orchestrator.toolResultMessage(context, toolCall.getId(), startTip, functionId);
                        toolMessage.setContent(startTip);
                    }
                    String msg = String.join("\n", list);
                    if (StrUtil.isNotBlank(msg)) {
                        orchestrator.toolResultMessage(context, toolCall.getId(), msg, functionId);
                        toolMessage.setContent(msg);
                    }

                    modelRunning.set(true);
                    try {
                        orchestrator.execute(cloneContext, submitMsg);
                    } finally {
                        modelRunning.set(false);
                    }
                    if (doneReceived.get() && queue.isEmpty()) {
                        consumerDone.complete(null);
                        return;
                    }
                }
            } finally {
                draining.set(false);
                if (!queue.isEmpty() && !consumerDone.isDone()) {
                    schedule();
                }
            }
        }
    }

    private AgentContext cloneContext(AgentContext context) {
        AgentContext cloneContext = BeanUtil.copyProperties(context, AgentContext.class);
        AgentExecutionSpec cloneRuntimeInfo = BeanUtil.copyProperties(context.getRuntimeInfo(), AgentExecutionSpec.class);
        ChatModelRequest cloneRequest = BeanUtil.copyProperties(context.getRuntimeInfo().getRequest(), ChatModelRequest.class);
        cloneRequest.setTools(null);
        cloneRuntimeInfo.setRequest(cloneRequest);
        cloneRuntimeInfo.setReflectAgentIds(Collections.emptyList());
        cloneContext.setRuntimeInfo(cloneRuntimeInfo);
        return cloneContext;
    }


    public record FunctionCallInfo(String callId, String functionName, String argument) {
    }
}
