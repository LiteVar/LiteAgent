package com.litevar.agent.rest.controller.external;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.dto.ExternalSendMsgDTO;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.agent.AgentApiKeyService;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.tool.executor.OpenToolThirdExecutor;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.SseStreamCoordinator;
import com.litevar.agent.rest.agentflow.listener.ExternalApiEventListener;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.springai.audio.SpeechService;
import com.litevar.opentool.model.StreamEventType;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * agent对外提供api
 *
 * @author uncle
 * @since 2025/4/1 10:30
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class AgentServerController {

    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private OpenToolThirdExecutor openToolThirdExecutor;
    @Autowired
    private ChatService chatService;
    @Autowired
    private SpeechService speechService;
    @Resource
    private AgentSessionManager manager;
    @Resource
    private SseStreamCoordinator chatStreamHandler;
    @Resource
    private AgentApiKeyService agentApiKeyService;
    @Resource
    private ExecutionStopManager stopManager;

    @IgnoreAuth
    @PostMapping("/initSession")
    public Object initSession(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        String agentId = getAgentIdFromToken(token);
        Agent agent = agentService.findById(agentId);

        List<String> datasetIds = agentDatasetRelaService.listDatasets(agentId).parallelStream().map(Dataset::getId).toList();
        String sessionId = manager.initSession(agent, datasetIds, 0, agent.getUserId(), AgentCallType.EXTERNAL.getCallType());
        return Dict.create().set("sessionId", sessionId);
    }

    @IgnoreAuth
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                                              @RequestParam("sessionId") String sessionId,
                                              @RequestBody @Valid ExternalSendMsgDTO dto) {
        getAgentIdFromToken(token);
        List<AgentSendMsgDTO> sendDto = BeanUtil.copyToList(dto.getContent(), AgentSendMsgDTO.class);
        boolean stream = dto.getIsChunk();
        return chatStreamHandler.streamChat(sessionId, stream, sendDto,
                (sink, requestId) -> new ExternalApiEventListener(sink, sessionId, requestId, stream));
    }

    /**
     * 音频转文字
     */
    @IgnoreAuth
    @PostMapping("/audio/transcriptions")
    public Object transcriptions(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                                 @RequestParam("audio") MultipartFile audio) {
        String agentId = getAgentIdFromToken(token);
        Agent agent = agentService.findById(agentId);
        if (agent == null) {
            throw new ServiceException(ServiceExceptionEnum.AGENT_NOT_EXIST_OR_NOT_SHARE);
        }
        if (StrUtil.isBlank(agent.getAsrModelId())) {
            throw new ServiceException("agent未配置ASR模型");
        }

        TokenReportDTO tokenReport = new TokenReportDTO(agent.getUserId(), agent.getAsrModelId(), agentId, "");

        String result = speechService.transcribe(tokenReport, agent.getAsrModelId(), audio);
        return JSONUtil.parse(result);
    }

    /**
     * 文字转音频
     */
    @IgnoreAuth
    @PostMapping(value = "/audio/speech", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> speech(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                                         @RequestParam String content) {
        String agentId = getAgentIdFromToken(token);
        Agent agent = agentService.findById(agentId);
        if (StrUtil.isBlank(agent.getTtsModelId())) {
            throw new ServiceException("agent未配置TTS模型");
        }

        TokenReportDTO tokenReport = new TokenReportDTO(agent.getUserId(), agent.getTtsModelId(), "", "");
        byte[] bytes = speechService.speech(tokenReport, agent.getTtsModelId(), content);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @IgnoreAuth
    @GetMapping("/version")
    public Object version(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        getAgentIdFromToken(token);
        return Dict.create().set("version", "3.0.0");
    }

    @IgnoreAuth
    @GetMapping("/clear")
    public Object clear(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                        @RequestParam("sessionId") String sessionId) {
        getAgentIdFromToken(token);
        manager.clearSession(sessionId);
        return Dict.create().set("id", sessionId);
    }

    @IgnoreAuth
    @PostMapping("/callback")
    public Object callback(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                           @RequestParam("sessionId") String sessionId,
                           @RequestBody @Valid CallbackParam data) {
        getAgentIdFromToken(token);
        manager.getSessionInfo(sessionId);
        String callId = data.getId();
        String result = JSONUtil.toJsonStr(data.getResult());
        log.info("收到第三方系统接口回调结果,callId:{},result:{}", callId, result);
        openToolThirdExecutor.callback(callId, result);

        return Dict.create().set("result", "success");
    }

    @IgnoreAuth
    @PostMapping("/streamCallback")
    public Object streamCallback(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                                 @RequestParam("sessionId") String sessionId,
                                 @RequestBody @Valid StreamCallbackParam data) {
        getAgentIdFromToken(token);
        manager.getSessionInfo(sessionId);
        StreamEventType eventType = StreamEventType.fromValue(data.getEvent());
        String callId = data.getToolReturn().getId();
        String result = JSONUtil.toJsonStr(data.getToolReturn().getResult());
        log.info("收到第三方系统流式回调结果,event:{},callId:{},result:{}", data.getEvent(), callId, result);
        openToolThirdExecutor.streamCallback(callId, eventType, result);

        return Dict.create().set("result", "success");
    }

    @IgnoreAuth
    @GetMapping("/history")
    public Object history(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                          @RequestParam("sessionId") String sessionId) {
        String agentId = getAgentIdFromToken(token);
        return chatService.agentSessionChat(agentId, sessionId);
    }

    @IgnoreAuth
    @GetMapping("/stop")
    public Object stop(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                       @RequestParam(value = "sessionId") String sessionId,
                       @RequestParam(value = "taskId", required = false) String taskId) {
        getAgentIdFromToken(token);
        stopManager.requestStop(taskId);
        return Dict.create().set("result", "success");
    }

    @IgnoreAuth
    @GetMapping("/agentInfo")
    public Object agentInfo(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        String agentId = getAgentIdFromToken(token);
        return agentService.apiAgentDetail(agentId);
    }

    public String getAgentIdFromToken(String token) {
        //token不是以Bearer开头，则响应回格式不正确
        if (!token.startsWith(CommonConstant.JWT_TOKEN_PREFIX)) {
            throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
        }
        try {
            String apiKey = token.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
            return agentApiKeyService.agentIdFromApiKey(apiKey);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
        }
    }


    @Data
    public static class CallbackParam {
        /**
         * call id
         */
        @NotBlank
        private String id;
        @NotNull
        private Object result;
    }

    @Data
    public static class StreamCallbackParam {
        @NotBlank
        private String event;
        @NotNull
        @Valid
        private StreamToolReturnParam toolReturn;
    }

    @Data
    public static class StreamToolReturnParam {
        @NotBlank
        private String id;
        @NotNull
        private Object result;
    }
}
