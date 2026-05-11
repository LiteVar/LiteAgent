package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.*;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.SseStreamCoordinator;
import com.litevar.agent.rest.agentflow.listener.WebClientEventListener;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.springai.audio.SpeechService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

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
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private SpeechService speechService;
    @Resource
    private AgentSessionManager manager;
    @Resource
    private SseStreamCoordinator chatStreamHandler;
    @Resource
    private ExecutionStopManager stopManager;

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
    public ResponseData<String> initSession(@PathVariable String agentId) {
        Agent agent = agentService.getById(agentId);
        if (agent == null) {
            agent = localAgentService.getById(agentId);
        }
        List<String> datasetIds = agentDatasetRelaService.listDatasets(agentId).parallelStream().map(Dataset::getId).toList();
        String sessionId = initSession(agent, 0, datasetIds);
        return ResponseData.success(sessionId);
    }

    private String initSession(Agent agent, Integer debugFlag, List<String> datasetIds) {
        String userId = LoginContext.currentUserId();

        return manager.initSession(agent, datasetIds, debugFlag, userId, AgentCallType.AGENT.getCallType());
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
    public Flux<ServerSentEvent<String>> stream(@PathVariable String sessionId,
                                                @RequestParam(value = "isChunk", defaultValue = "true", required = false) Boolean stream,
                                                @RequestBody @Valid List<AgentSendMsgDTO> dto) {
        return chatStreamHandler.streamChat(sessionId, stream, dto,
            (sink, requestId) -> new WebClientEventListener(sink, requestId, stream));
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
        manager.clearSession(sessionId);
        return ResponseData.success();
    }

    /**
     * 停止执行
     *
     * @param taskId 推送的第一条消息的taskId
     * @return
     */
    @PostMapping("/stop")
    public ResponseData<String> stop(@RequestParam("taskId") String taskId) {
        stopManager.requestStop(taskId);
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
    public ResponseData<MessageAndClearDTO> agentChat(@PathVariable String agentId,
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
                                                         @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
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
     */
    @PostMapping("/audio/transcriptions")
    public ResponseData<String> transcriptions(
        @RequestParam(required = false, defaultValue = "") String agentId,
        @RequestParam String modelId,
        @RequestParam("audio") MultipartFile audio
    ) {
        TokenReportDTO tokenReport = new TokenReportDTO(LoginContext.currentUserId(), modelId, agentId, "");
        return ResponseData.success(speechService.transcribe(tokenReport, modelId, audio));
    }

    /**
     * 音频转文字-流式
     *
     * @param modelId
     * @param audio
     * @return
     */
    @PostMapping(value = "/audio/transcriptions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> transcriptionStream(
            @RequestParam(required = false, defaultValue = "") String agentId,
            @RequestParam String modelId,
            @RequestParam("audio") MultipartFile audio
    ) {
        TokenReportDTO tokenReport = new TokenReportDTO(LoginContext.currentUserId(), modelId, agentId, "");
        return Flux.just(ServerSentEvent.<String>builder()
                        .event("init")
                        .data("connected")
                        .build())
                .concatWith(Flux.defer(() -> speechService.transcribeStream(tokenReport, modelId, audio))
                        .doOnNext(s -> log.info("transcription chunk: {}", s))
                        .timeout(Duration.ofSeconds(40))
                        .map(data -> ServerSentEvent.builder(data).build())
                        .concatWithValues(ServerSentEvent.builder("[DONE]").build())
                        .doOnError(TimeoutException.class, ex -> log.warn("transcriptionStream SSE连接超时: modelId={}, agentId={}", modelId, agentId))
                        .onErrorResume(TimeoutException.class,
                                ex -> Flux.just(ServerSentEvent.<String>builder()
                                        .event("timeout")
                                        .data("连接超时,请重新发起请求")
                                        .build()))
                        .onErrorResume(ex -> Flux.just(buildErrorEvent(ex, json -> json))));
    }

    /**
     * 文字转音频
     *
     * @param agentId 可选参数，提供agentId可以让系统记录token使用情况
     * @param modelId 必填参数，指定使用哪个模型进行文字转音频
     * @param content 必填参数，输入的文本内容
     */
    @PostMapping(value = "/audio/speech", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> speech(
        @RequestParam(required = false, defaultValue = "") String agentId,
        @RequestParam String modelId,
        @RequestParam String content
    ) {
        TokenReportDTO tokenReport = new TokenReportDTO(LoginContext.currentUserId(), modelId, agentId, "");
        byte[] data = speechService.speech(tokenReport, modelId, content);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }

    /**
     * 文字转音频-流式
     *
     * @param modelId
     * @param content
     * @param agentId
     * @return
     */
    @PostMapping(value = "/audio/speech/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> speechStream(
        @RequestParam(required = false, defaultValue = "") String agentId,
        @RequestParam String modelId,
        @RequestParam String content
    ) {
        TokenReportDTO tokenReport = new TokenReportDTO(LoginContext.currentUserId(), modelId, agentId, "");
        return Flux.defer(() -> speechService.speechStream(tokenReport, modelId, content))
                .map(data -> ServerSentEvent.builder(data).build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").build())
                .onErrorResume(ex -> Flux.just(buildErrorEvent(ex, Function.identity())));
    }

    private ResponseData<String> buildStreamError(Throwable ex) {
        if (ex instanceof ServiceException serviceException) {
            return ResponseData.error(serviceException.getCode(), serviceException.getMessage());
        }
        if (ex instanceof StreamException streamException) {
            return ResponseData.error(streamException.getCode(), streamException.getMessage());
        }
        return ResponseData.error(500, ex.getMessage());
    }

    private <T> ServerSentEvent<T> buildErrorEvent(Throwable ex, Function<String, T> dataMapper) {
        String json = JSONUtil.toJsonStr(buildStreamError(ex));
        return ServerSentEvent.<T>builder()
            .event("error")
            .data(dataMapper.apply(json))
            .build();
    }

}
