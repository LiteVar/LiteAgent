package com.litevar.agent.rest.agentflow.agent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.*;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.executor.TaskExecutor;
import com.litevar.agent.rest.util.TypewriterEffectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

/**
 * orchestration engine.
 *
 * @author uncle
 * @since 2026/1/28 13:06
 */
@Slf4j
@Component
public class Orchestrator extends MessageEmitter {

    @Resource
    private AgentEngine engine;
    @Resource
    private ToolDispatcher toolDispatcher;
    @Resource
    private ChatContext chatContext;
    @Resource
    private ExecutionStopManager stopManager;
    @Lazy
    @Resource
    private AgentSessionManager manager;
    @Resource
    private ReflectionAgent reflectionAgent;

    /**
     * 新taskId
     */
    public CompletionResponse newTaskChat(AgentContext parentContext, String agentId, String taskId, List<Message> submitMsg) {
        AgentContext context = new AgentContext();
        context.setSessionId(parentContext.getSessionId());
        context.setAgentId(agentId);
        context.setParentTaskId(StrUtil.equals(parentContext.getTaskId(), parentContext.getParentTaskId()) ? null : parentContext.getTaskId());
        context.setTaskId(taskId);
        context.setRequestId(parentContext.getRequestId());
        context.setUserId(parentContext.getUserId());
        context.setStream(parentContext.isStream());
        AgentExecutionSpec runtimeInfo = manager.getAgentRuntimeInfo(parentContext.getSessionId(), agentId);
        context.setRuntimeInfo(runtimeInfo);

        Callable<Object> task = () -> execute(context, submitMsg);

        try {
            String contextId = context.getRuntimeInfo().getRequest().getContextId();
            Integer executeMode = context.getRuntimeInfo().getExecuteMode();
            CompletableFuture<Object> future = TaskExecutor.execute(contextId, executeMode, task);
            return (CompletionResponse) future.join();
        } catch (Exception e) {
            //reject模式:不能执行时直接以error返回
            handleError(context, e);
            return null;
        } finally {
            chatContext.taskDone(context.getSessionId(), agentId, taskId);
        }
    }

    public CompletionResponse execute(AgentContext context, List<Message> messages) {
        AgentExecutionSpec runtimeInfo = context.getRuntimeInfo();
        CompletionResponse response = decide(context, messages);
        while (response != null) {
            if (stopManager.shouldStop(context.getRequestId())) {
                break;
            }
            AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
            if (StrUtil.isNotBlank(assistantMessage.getReasoningContent())) {
                thinkMessage(context, response);
            }
            if (response.isFunctionCalling()) {
                if (StrUtil.isNotBlank(assistantMessage.getContent())) {
                    //function calling时有文本输出的情况
                    CompletionResponse clone = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
                    clone.setUsage(null);
                    textMessage(context, clone);
                }
                List<Message> result = toolDispatcher.dispatch(context, response);
                if (!result.isEmpty()) {
                    if (ObjectUtil.equal(runtimeInfo.getAgentType(), AgentType.DISTRIBUTE.getType())) {
                        AssistantMessage fcMsg = (AssistantMessage) result.get(0);
                        //只有分发agent的情况,本次task结束
                        boolean isAllDistribute = fcMsg.getToolCalls().stream()
                                .allMatch(i -> StrUtil.equals(i.getFunction().getName(), AgentRuntimeFactory.TOOL_AGENT_DISTRIBUTE));
                        if (isAllDistribute) {
                            chatContext.addTaskMessage(runtimeInfo.getRequest().getContextId(), context.getTaskId(), result);
                            break;
                        }
                    }
                    response = decide(context, result);
                } else {
                    break;
                }
            } else {
                Runnable beforeStopTask = beforeStop(response, context);
                if (response.isStopByLength()) {
                    //模型输出达到max token中断
                    if (StrUtil.isNotBlank(response.getChoices().get(0).getMessage().getContent())) {
                        beforeStopTask.run();
                    }
                    handleError(context, new ServiceException(ServiceExceptionEnum.TOKEN_LIMIT_REACHED,
                            "agent:" + context.getRuntimeInfo().getAgentName() + ",max_completion_tokens:" + context.getRuntimeInfo().getRequest().getMaxCompletionTokens()));
                    response = null;
                    break;
                }
                if (stopManager.shouldStop(context.getRequestId())) {
                    beforeStopTask.run();
                    break;
                }
                List<String> inputTextList = extraText(messages);
                String reflectionAction = reflectionAgent.reflect(context, StrUtil.join(" ", inputTextList), response);
                if (stopManager.shouldStop(context.getRequestId())) {
                    beforeStopTask.run();
                    break;
                }
                if (reflectionAction != null) {
                    response = decide(context, List.of(UserMessage.of(reflectionAction)));
                } else {
                    beforeStopTask.run();
                    break;
                }
            }
        }
        return response;
    }

    void handleError(AgentContext context, Throwable e) {
        if (stopManager.shouldStop(context.getRequestId())) {
            return;
        }
        AgentExecutionSpec runtimeInfo = context.getRuntimeInfo();
        if (ObjectUtil.notEqual(runtimeInfo.getAgentType(), AgentType.REFLECTION.getType())) {
            errorMessage(context, e);
        }
    }

    private CompletionResponse decide(AgentContext context, List<Message> messages) {
        return decide(context, messages, true);
    }


    private CompletionResponse decide(AgentContext context, List<Message> messages, boolean allowRetry) {
        //用于底层stream流片段向上层输出
        BiConsumer<String, Integer> onChunk = (part, chunkType) -> {
            if (stopManager.shouldStop(context.getRequestId())) {
                return;
            }
            if (chunkType == 1 || (ObjectUtil.notEqual(context.getRuntimeInfo().getAgentType(), AgentType.REFLECTION.getType())
                    && ObjectUtil.isEmpty(context.getRuntimeInfo().getReflectAgentIds()))) {
                chunkMessage(context, part, chunkType);
            }
        };
        try {
            return engine.decide(context, messages, onChunk);
        } catch (Exception e) {
            Throwable error = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            if (allowRetry && StrUtil.isNotEmpty(error.getMessage())
                    && error.getMessage().contains(AgentEventListener.ERROR_TOKEN)) {
                //上下文超了=>摘要当前task上下文后,重新发一次请求
//                boolean success = chatContext.compressTask(context.getSessionId(), context.getAgentId(), context.getTaskId());
//                if (success) {
//                    return decide(context, messages, false);
//                }
            }
            handleError(context, error);
            return null;
        }
    }

    private List<String> extraText(List<Message> messages) {
        List<String> inputTextList = new ArrayList<>();
        for (Message msg : messages) {
            if (msg instanceof UserMessage userMessage) {
                if (userMessage.getContent() instanceof String content) {
                    inputTextList.add(content);
                } else if (userMessage.getContent() instanceof List<?> list) {
                    for (Object i : list) {
                        if (i instanceof UserMessage.TextContentType content) {
                            inputTextList.add(content.getText());
                        }
                    }
                }
                break;
            }
        }
        return inputTextList;
    }

    private Runnable beforeStop(CompletionResponse response, AgentContext context) {
        return () -> {
            if (ObjectUtil.isNotEmpty(context.getRuntimeInfo().getReflectAgentIds()) && context.isStream()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                TypewriterEffectUtil.part(content, 50, part -> chunkMessage(context, part, 0));
            }
            textMessage(context, response);
        };
    }
}
