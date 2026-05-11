package com.litevar.agent.rest.agentflow;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.core.module.storage.ChatTempFileService;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.ToolMessage;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.agent.Orchestrator;
import com.litevar.agent.rest.agentflow.auto.execution.PlanExecutionBootstrap;
import com.litevar.agent.rest.agentflow.auto.execution.PlanExecutionState;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.agentflow.bean.SessionInfo;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventBus;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.agentflow.listener.DisconnectableEventListener;
import com.litevar.agent.rest.agentflow.message.UserSendEvent;
import com.litevar.agent.rest.config.StorageProperties;
import com.litevar.agent.rest.service.UploadFileServiceV2;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/**
 * SSE chat stream handling
 *
 * @author uncle
 * @since 2025/12/23 12:31
 */
@Slf4j
@Component
public class SseStreamCoordinator {

    @Resource
    private AgentEventBus bus;
    @Resource
    private AgentSessionManager manager;
    @Resource
    private Orchestrator orchestrator;
    @Resource
    private UploadFileServiceV2 uploadFileServiceV2;
    @Resource
    private ChatTempFileService chatTempFileService;
    @Resource
    private Scheduler customScheduler;
    @Resource
    private ExecutionStopManager stopManager;
    @Resource
    private StorageProperties storageProperties;
    @Resource
    private PlanExecutionBootstrap planBootstrap;

    public <T extends AgentEventListener & DisconnectableEventListener> Flux<ServerSentEvent<String>> streamChat(
            String sessionId,
            boolean stream,
            List<AgentSendMsgDTO> sendDto,
            BiFunction<FluxSink<ServerSentEvent<String>>, String, T> listenerFactory) {
        return Flux.<ServerSentEvent<String>>create(sink -> {
                    String requestId = IdUtil.getSnowflakeNextIdStr();
                    Runnable task;
                    AgentContext context = new AgentContext();
                    try {
                        task = beforeChat(context, sendDto, sessionId, requestId, stream);
                    } catch (ServiceException ex) {
                        log.error("业务异常", ex);
                        sink.error(new StreamException(ex.getCode(), ex.getMessage()));
                        return;
                    } catch (Exception ex) {
                        log.error("业务异常", ex);
                        sink.error(new StreamException(500, ex.getMessage()));
                        return;
                    }
                    T listener = listenerFactory.apply(sink, requestId);
                    bus.register(requestId, listener);
                    AtomicBoolean closed = new AtomicBoolean(false);
                    Runnable cleanup = () -> {
                        if (closed.compareAndSet(false, true)) {
                            afterChat(requestId, listener);
                        }
                    };

                    sink.onCancel(() -> {
                        log.info("客户端主动取消SSE连接: sessionId={}, requestId={}", sessionId, requestId);
                        cleanup.run();
                    });

                    customScheduler.schedule(() -> {
                        try {
                            stopManager.init(requestId);
                            //回推消息给client
                            bus.publish(new AgentEvent(context, new UserSendEvent(sendDto)));

                            //start chat
                            task.run();
                        } catch (Exception ex) {
                            sink.error(ex);
                        } finally {
                            cleanup.run();
                        }
                    });


                }).timeout(Duration.ofMinutes(10))
                .doOnError(TimeoutException.class, e -> log.warn("SSE连接超时: sessionId={}", sessionId))
                .onErrorResume(TimeoutException.class,
                        e -> Flux.just(ServerSentEvent.<String>builder().event("timeout").data("连接超时,请重新发起请求").build()));
    }

    /**
     * Chat request preparation.
     */
    private Runnable beforeChat(AgentContext context, List<AgentSendMsgDTO> sendDto, String sessionId, String requestId, boolean stream) {
        SessionInfo sessionInfo = manager.getSessionInfo(sessionId);

        context.setSessionId(sessionId);
        context.setRequestId(requestId);
        context.setUserId(sessionInfo.getUserId());
        context.setStream(stream);
        context.setAgentId(sessionInfo.getAgentId());
        //第一级agent的taskId=requestId
        context.setTaskId(requestId);
        context.setParentTaskId(requestId);

        //判断是否是自动创建agent的确认指令
        Optional<String> planOpt = sendDto.stream()
                .filter(i -> StrUtil.equals(i.getType(), AgentSendMsgDTO.MessageType.EXECUTE.getType()))
                .map(AgentSendMsgDTO::getMessage).findFirst();
        List<Message> submitMsg = planOpt.map(i -> buildAutoAgentMsg(i, context))
                .orElseGet(() -> buildModelMsg(sendDto, context));
        if (planOpt.isPresent()) {
            AgentSendMsgDTO executeMsg = new AgentSendMsgDTO();
            executeMsg.setType(AgentSendMsgDTO.MessageType.TEXT.getType());
            executeMsg.setMessage("执行方案");
            sendDto.clear();
            sendDto.add(executeMsg);
        }
        return () -> orchestrator.newTaskChat(context, sessionInfo.getAgentId(), requestId, submitMsg);
    }

    private List<Message> buildAutoAgentMsg(String planId, AgentContext context) {
        PlanExecutionState state = planBootstrap.initState(context.getSessionId(), planId);
        if (state == null) {
            throw new StreamException(404, "找不到该规划执行信息");
        }

        PlanExecutionBootstrap.PlanInfo planInfo = planBootstrap.loadPlan(planId);

        //还原触发PlanningAgent的functionCalling请求继续往下走
        AssistantMessage.Function function = new AssistantMessage.Function();
        function.setName(AgentRuntimeFactory.TOOL_AUTO_AGENT);
        function.setArguments(JSONUtil.toJsonStr(Dict.create().set("task",planInfo.getOriginTask())));

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall();
        toolCall.setId(planId);
        toolCall.setType("function");
        toolCall.setFunction(function);

        AssistantMessage assistantMessage = new AssistantMessage();
        assistantMessage.setToolCalls(List.of(toolCall));

        ToolMessage toolMessage = new ToolMessage();
        toolMessage.setToolCallId(planId);
        toolMessage.setContent(JSONUtil.toJsonStr(Dict.create()
                .set("planId", planId)
                .set("agentList", state.getAgents().stream().map(i -> Dict.create()
                        .set("agentId", i.getAgentId())
                        .set("agentName", i.getAgentName())
                        .set("duty", i.getDuty())
                        .set("constraint", i.getConstraint())
                        .set("status", i.getStatus().name())
                        .set("dependencyAgentIds", i.getDependencyAgentIds())
                ).toList())));

        List<Message> submitMsg = new ArrayList<>();
        submitMsg.add(assistantMessage);
        submitMsg.add(toolMessage);
        return submitMsg;
    }

    private List<Message> buildModelMsg(List<AgentSendMsgDTO> sendDto, AgentContext context) {
        long imageCount = sendDto.stream()
                .map(dto -> Objects.requireNonNull(AgentSendMsgDTO.MessageType.of(dto.getType())))
                .filter(type -> type == AgentSendMsgDTO.MessageType.IMAGE_URL)
                .count();
        if (imageCount > 10) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID.getCode(), "图片最多支持10张");
        }
        long videoCount = sendDto.stream()
                .map(dto -> Objects.requireNonNull(AgentSendMsgDTO.MessageType.of(dto.getType())))
                .filter(type -> type == AgentSendMsgDTO.MessageType.VIDEO_URL)
                .count();
        if (videoCount > 1) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID.getCode(), "视频最多支持1个");
        }
        AgentExecutionSpec runtimeInfo = manager.getAgentRuntimeInfo(context.getSessionId(), context.getAgentId());
        boolean supportVision = runtimeInfo.getVision();
        boolean multiMedia = imageCount > 0 || videoCount > 0;

        List<Message> submitMsg = new ArrayList<>();

        List<UserMessage.ContentType> contents = new ArrayList<>();
        for (AgentSendMsgDTO dto : sendDto) {
            switch (Objects.requireNonNull(AgentSendMsgDTO.MessageType.of(dto.getType()))) {
                case IMAGE_URL -> {
                    String content = dto.getMessage();
                    boolean isUrl = Validator.isUrl(content);
                    if (!isUrl) {
                        String fileKey = storeTempFile(dto.getMessage(), storageProperties.getImagePath());
                        String localUrl = uploadFileServiceV2.generateNoExpireFileUrl(fileKey);
                        content = supportVision ? uploadFileServiceV2.fileBase64Str(fileKey) : localUrl;
                        dto.setMessage(localUrl);
                    }
                    if (supportVision) {
                        contents.add(UserMessage.ImageContentType.of(content));
                    } else {
                        submitMsg.add(UserMessage.of(content));
                    }
                }
                case VIDEO_URL -> {
                    String content = dto.getMessage();
                    boolean isUrl = Validator.isUrl(content);
                    if (!isUrl) {
                        String fileKey = storeTempFile(dto.getMessage(), storageProperties.getVideoPath());
                        String localUrl = uploadFileServiceV2.generateNoExpireFileUrl(fileKey);
                        content = supportVision ? uploadFileServiceV2.fileBase64Str(fileKey) : localUrl;
                        dto.setMessage(localUrl);
                    }
                    if (supportVision) {
                        contents.add(UserMessage.VideoContentType.of(content));
                    } else {
                        submitMsg.add(UserMessage.of(content));
                    }
                }
                default -> {
                    if (supportVision && multiMedia) {
                        contents.add(UserMessage.TextContentType.of(dto.getMessage()));
                    } else {
                        submitMsg.add(UserMessage.of(dto.getMessage()));
                    }
                }
            }
        }

        if (!contents.isEmpty()) {
            submitMsg.add(UserMessage.of(contents));
        }

        return submitMsg;
    }

    private String storeTempFile(String fileId, String uploadPath) {
        ChatTempFileService.TempFileData fileData = chatTempFileService.openTempFile(fileId);
        //fileKey
        return uploadFileServiceV2.uploadFile(fileData.inputStream(), uploadPath, fileData.filename(), fileData.mime());
    }

    private void afterChat(String requestId, DisconnectableEventListener listener) {
        listener.disconnect();
        bus.unregister(requestId);
        //如果agent正在输出,要停止
        stopManager.requestStop(requestId);
        stopManager.clear(requestId);
    }
}
