package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.*;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.openai.agent.AgentMsgType;
import com.litevar.agent.rest.openai.agent.MultiAgent;
import com.litevar.agent.rest.openai.handler.AgentMessageHandler;
import com.litevar.agent.rest.openai.handler.SseClientMessageHandler;
import com.litevar.agent.rest.openai.message.UserSendMessage;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.springai.audio.SpeechService;
import com.litevar.agent.rest.util.AgentUtil;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * @author uncle
 * @since 2024/8/27 11:08
 */
@Slf4j
@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    @Autowired
    private AgentService agentService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private LocalAgentService localAgentService;
    @Autowired
    private AgentUtil agentUtil;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private SpeechService speechService;

    /**
     * 初始化会话
     * <strong>适用于调试</strong>
     *
     * @return
     */
    @PostMapping("/initSession")
    public ResponseData<String> initSession(@RequestBody @Valid AgentDebugDTO dto) {
        Agent agent = agentService.findById(dto.getAgentId());
        BeanUtil.copyProperties(dto, agent);
        agent.setLlmModelId(dto.getModelId());

        String sessionId = initSession(agent, 1, dto.getDatasetIds());
        return ResponseData.success(sessionId);
    }

    /**
     * 初始化会话
     *
     * @param agentId
     * @return
     */
    @PostMapping("/initSession/{agentId}")
    public ResponseData<String> initSession(@PathVariable("agentId") String agentId) {
        Agent agent = agentService.getById(agentId);
        if (agent == null) {
            agent = localAgentService.getById(agentId);
        }
        List<String> datasetIds = agentDatasetRelaService.listDatasets(agentId).parallelStream().map(Dataset::getId).toList();
        String sessionId = initSession(agent, 0, datasetIds);
        return ResponseData.success(sessionId);
    }

    private String initSession(Agent agent, Integer debugFlag, List<String> datasetIds) {
        //反思agent不支持直接聊天
        if (Objects.equals(agent.getType(), AgentType.REFLECTION.getType())) {
            throw new ServiceException(ServiceExceptionEnum.REFLECT_AGENT_CANNOT_CHAT);
        }

        String userId = LoginContext.currentUserId();
        return AgentManager.initSession(agent, datasetIds, debugFlag, userId, AgentCallType.AGENT.getCallType());
    }

    /**
     * 发送会话消息
     *
     * @param sessionId 会话id
     * @param stream    是否分块流式传输
     * @param dto       消息内容
     * @return 响应式流
     */
    @PostMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable("sessionId") String sessionId,
                                                @RequestParam(value = "isChunk", defaultValue = "true", required = false) Boolean stream,
                                                @RequestBody @Valid List<AgentSendMsgDTO> dto) {
        return Flux.<ServerSentEvent<String>>create(sink -> {
                    //第一级agent的taskId=requestId
                    String requestId = IdUtil.getSnowflakeNextIdStr();
                    SseClientMessageHandler messageHandler = new SseClientMessageHandler(sessionId, requestId, sink, stream);
                    final List<AgentPlanningDTO> taskList = new ArrayList<>();

                    try {
                        Optional<AgentSendMsgDTO> opt = dto.stream().filter(i -> StrUtil.equals(i.getType(), "execute")).findFirst();
                        List<AgentMessageHandler> handlers = AgentManager.getHandler(sessionId);
                        if (opt.isPresent()) {
                            String planId = opt.get().getMessage();
                            List<AgentPlanningDTO> cacheTaskList = (List<AgentPlanningDTO>) RedisUtil.getValue(String.format(CacheKey.SESSION_PLAN_INFO, planId));
                            if (cacheTaskList == null) {
                                sink.error(new StreamException(404, "找不到该规划信息"));
                                return;
                            }
                            taskList.addAll(cacheTaskList);

                            AgentSendMsgDTO executeMsg = new AgentSendMsgDTO();
                            executeMsg.setType("text");
                            executeMsg.setMessage("执行方案");
                            dto.clear();
                            dto.add(executeMsg);
                            RedisUtil.delKey(String.format(CacheKey.SESSION_PLAN_INFO, planId));
                        }
                        handlers.add(messageHandler);
                    } catch (ServiceException ex) {
                        sink.error(new StreamException(ex.getCode(), ex.getMessage()));
                        return;
                    } catch (Exception ex) {
                        sink.error(new StreamException(500, ex.getMessage()));
                        return;
                    }

                    // 设置取消处理器,确保资源清理
                    sink.onCancel(() -> {
                        log.info("SSE连接被取消: sessionId={}, requestId={}", sessionId, requestId);
                        afterChat(sessionId, requestId);
                    });

                    // 响应式异步处理
                    Mono.fromRunnable(() -> {
                                MultiAgent agent = AgentManager.getAgent(sessionId);
                                String taskId = requestId;
                                UserSendMessage userSendMessage = new UserSendMessage(sessionId, taskId, agent.getAgentId(), dto, requestId);
                                AgentManager.handleMsg(AgentMsgType.USER_SEND_MSG, userSendMessage);

                                CurrentAgentRequest.AgentRequest currentContext = new CurrentAgentRequest.AgentRequest();
                                currentContext.setSessionId(sessionId);
                                currentContext.setParentTaskId(null);
                                currentContext.setTaskId(null);
                                currentContext.setRequestId(requestId);
                                currentContext.setAgentId(agent.getAgentId());
                                CurrentAgentRequest.setContext(currentContext);

                                if (!taskList.isEmpty()) {
                                    List<MultiAgent> taskAgentList;
                                    try {
                                        taskAgentList = agentUtil.createAgent(taskList);
                                    } catch (Exception e) {
                                        currentContext.setTaskId(taskId);
                                        agent.handleError(currentContext, new ServiceException("agent组装失败"));
                                        return;
                                    }

                                    String result = agentUtil.executeAgent(taskAgentList, stream);
                                    agentUtil.summary(result, agent, stream);

                                } else {
                                    List<Message> submitMsg = new ArrayList<>();
                                    for (AgentSendMsgDTO msgDto : dto) {
                                        submitMsg.add(UserMessage.of(msgDto.getMessage()));
                                    }
                                    AgentManager.chat(agent, taskId, submitMsg, stream);
                                }
                                afterChat(sessionId, requestId);
                            }).subscribeOn(Schedulers.boundedElastic())
                            .subscribe(null, sink::error, () -> afterChat(sessionId, requestId));
                })
                .timeout(Duration.ofMinutes(10))
                .doOnError(TimeoutException.class, e -> log.warn("SSE连接超时: sessionId={}", sessionId))
                .onErrorResume(TimeoutException.class, e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("timeout")
                        .data("连接超时，请重新发起请求")
                        .build()));
    }

    private void afterChat(String sessionId, String requestId) {
        AgentManager.getHandler(sessionId).removeIf(h -> {
            if (h instanceof SseClientMessageHandler handler && StrUtil.equals(handler.getRequestId(), requestId)) {
                handler.disconnect(requestId);
                return true;
            }
            return false;
        });
    }

    /**
     * 清空会话信息
     * 清空上下文时调用
     *
     * @param sessionId
     * @return
     */
    @PostMapping("/clearSession")
    public ResponseData<String> clearSession(@RequestParam("sessionId") String sessionId) {
        AgentManager.clearSession(sessionId);
        return ResponseData.success();
    }

    /**
     * 清空聊天记录
     *
     * @param agentId
     * @return
     */
    @PostMapping("/clearDebugRecord")
    public ResponseData<String> clearDebugRecord(@RequestParam("agentId") String agentId,
                                                 @RequestParam(value = "debugFlag", defaultValue = "1") Integer debugFlag) {
        chatService.clearChatData(agentId, debugFlag);
        return ResponseData.success();
    }

    /**
     * 最近聊天的agent
     *
     * @param workspaceId
     * @return
     */
    @GetMapping("/recentAgent")
    public ResponseData<List<AgentSessionVO>> recentAgent(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        List<AgentSessionVO> list = chatService.recentAgent(workspaceId);
        return ResponseData.success(list);
    }

    /**
     * agent聊天记录
     *
     * @param agentId
     * @param debugFlag 0-正常聊天,1-调试
     * @param sessionId 这里sessionId用于游标分页,第一页传空
     * @return
     */
    @GetMapping("/agentChat/{agentId}")
    public ResponseData<MessageAndClearDTO> agentChat(@PathVariable("agentId") String agentId,
                                                      @RequestParam(value = "sessionId", required = false) String sessionId,
                                                      @RequestParam(value = "debugFlag", defaultValue = "0") Integer debugFlag,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        MessageAndClearDTO dto = chatService.agentChat(agentId, sessionId, debugFlag, pageSize);
        return ResponseData.success(dto);
    }

    /**
     * agent聊天记录
     * 该agent在本系统调试端、用户端、api调用产生的数据
     *
     * @param agentId  agentId
     * @param pageSize 默认为10
     * @param pageNo   默认为0
     * @return
     */
    @GetMapping("/agentChat")
    public ResponseData<PageModel<MessageDTO>> agentChat(@RequestParam("agentId") String agentId,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                         @RequestParam(value = "pageNo", defaultValue = "0") Integer pageNo,
                                                         @RequestParam(value = "sessionId", required = false) String sessionId) {
        PageModel<MessageDTO> res = chatService.agentChat(agentId, pageNo, pageSize, sessionId);
        return ResponseData.success(res);
    }

    /**
     * 音频转文字
     *
     * @param modelId
     * @param audio
     * @return
     * @throws IOException
     */
    @PostMapping("/audio/transcriptions")
    public ResponseData<String> transcriptions(
            @RequestParam String modelId,
            @RequestParam("audio") MultipartFile audio
    ) {
        return ResponseData.success(speechService.transcriptions(modelId, audio));
    }

    /**
     * 文字转音频
     *
     * @param modelId
     * @param content
     * @param response
     */
    @RequestMapping(value = "/audio/speech", method = {RequestMethod.GET, RequestMethod.POST})
    public void speech(
            @RequestParam String modelId,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "false") Boolean stream,
            HttpServletResponse response
    ) {
        byte[] bytes;
        if (stream) {
            bytes = speechService.speechStream(modelId, content);
        } else {
            bytes = speechService.speech(modelId, content);
        }

        String fileName = System.currentTimeMillis() + ".wav";
        // 设置响应头，指定内容类型为MP3音频
        response.setContentType("audio/wav");
        // 设置Content-Disposition头，使浏览器将响应作为下载处理
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 将音频数据从输入流复制到响应输出流
        try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Error while processing audio response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
