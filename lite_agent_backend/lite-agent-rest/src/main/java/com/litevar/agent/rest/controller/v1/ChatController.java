package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentDebugDTO;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.dto.AgentToModelDTO;
import com.litevar.agent.base.dto.MessageDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.openai.OpenAIClientUtil;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.openai.agent.AgentMsgType;
import com.litevar.agent.rest.openai.agent.MultiAgent;
import com.litevar.agent.rest.openai.handler.AgentMessageHandler;
import com.litevar.agent.rest.openai.handler.SseClientMessageHandler;
import com.litevar.agent.rest.openai.message.UserSendMessage;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.util.AgentUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
        AgentToModelDTO agentParam = agentUtil.buildSessionParam(agent, datasetIds);
        return AgentManager.initSession(agentParam, debugFlag, userId, AgentCallType.AGENT.getCallType());
    }

    /**
     * 发送会话消息
     *
     * @param sessionId 会话id
     * @param dto
     * @return
     */
    @PostMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable("sessionId") String sessionId, @RequestBody @Valid List<AgentSendMsgDTO> dto) {
        SseEmitter sseEmitter = new SseEmitter(1000L * 60 * 10);
        String taskId = IdUtil.getSnowflakeNextIdStr();
        boolean stream = true;
        SseClientMessageHandler messageHandler = new SseClientMessageHandler(sessionId, taskId, sseEmitter, stream);
        try {
            List<AgentMessageHandler> handlers = AgentManager.getHandler(sessionId);
            handlers.add(messageHandler);
        } catch (ServiceException ex) {
            //这里有可能是session过期/丢失的情况
            sseEmitter.completeWithError(new StreamException(ex.getCode(), ex.getMessage()));
            return sseEmitter;
        } catch (Exception ex) {
            sseEmitter.completeWithError(new StreamException(500, ex.getMessage()));
            return sseEmitter;
        }

        //将用户发送消息的回推给客户端
        MultiAgent agent = AgentManager.getAgent(sessionId);
        UserSendMessage userSendMessage = new UserSendMessage(sessionId, taskId, agent.getAgentId(), dto);
        AgentManager.handleMsg(AgentMsgType.USER_SEND_MSG, userSendMessage);

        List<Message> submitMsg = new ArrayList<>();
        for (AgentSendMsgDTO msg : dto) {
            submitMsg.add(UserMessage.of(msg.getMessage()));
        }

        CompletableFuture.runAsync(() -> AgentManager.chat(agent, taskId, submitMsg, stream));

        return sseEmitter;
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
     * @return
     */
    @GetMapping("/agentChat/{agentId}")
    public ResponseData<List<MessageDTO>> agentChat(@PathVariable("agentId") String agentId,
                                                    @RequestParam(value = "debugFlag", defaultValue = "0") Integer debugFlag) {
        List<MessageDTO> list = chatService.agentChat(agentId, debugFlag);
        return ResponseData.success(list);
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
    ) throws IOException {
        return ResponseData.success(OpenAIClientUtil.transcriptions(modelId, audio));
    }

    /**
     * 文字转音频
     *
     * @param modelId
     * @param content
     * @param response
     */
    @PostMapping(value = "/audio/speech")
    public void speech(
        @RequestParam String modelId,
        @RequestParam String content,
        HttpServletResponse response
    ) {
        try (InputStream inputStream = OpenAIClientUtil.speech(modelId, content)) {
            // 设置响应头
            response.setContentType("audio/mp3"); // 根据实际音频格式设置
            response.setHeader("Content-Disposition", "attachment; filename=\"" + System.currentTimeMillis() + ".mp3\"");

            String filename = URLEncoder.encode(System.currentTimeMillis() + ".mp3", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
//            response.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + filename);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(inputStream.readAllBytes());
            outputStream.flush();
        } catch (IOException e) {
            log.error("Error while processing audio response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
