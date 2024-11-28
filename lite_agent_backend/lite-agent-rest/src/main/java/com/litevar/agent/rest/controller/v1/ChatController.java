package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentDebugDTO;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.agent.LocalAgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.rest.langchain4j.AiClientManager;
import com.litevar.agent.rest.langchain4j.handler.SseClientMessageHandler;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2024/8/27 11:08
 */
@Slf4j
@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    @Autowired
    private ModelService modelService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private LocalAgentService localAgentService;

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

        String sessionId = initSession(agent, 1);
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
        String sessionId = "";
        LocalAgent localAgent = localAgentService.agentById(agentId);
        if (localAgent != null) {
            sessionId = initSession(localAgent);
        } else {
            Agent agent = agentService.findById(agentId);
            sessionId = initSession(agent, 0);
        }
        return ResponseData.success(sessionId);
    }

    private String initSession(LocalAgent localAgent) {
        if (StrUtil.isEmpty(localAgent.getLlmModelId())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }
        LlmModel model = localAgentService.modelById(localAgent.getLlmModelId());
        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }
        List<ToolProvider> toolList = null;
        List<ToolFunction> functionList = null;
        if (ObjectUtil.isNotEmpty(localAgent.getToolIds())) {
            toolList = localAgentService.toolByIds(localAgent.getToolIds())
                    .stream().map(i -> (ToolProvider) i).toList();
            functionList = localAgentService.functionByToolIds(localAgent.getToolIds());
        }

        String sessionId = AiClientManager.initSession(model, functionList, localAgent.getPrompt(),
                localAgent.getTemperature(), localAgent.getTopP(), localAgent.getMaxTokens());

        chatService.cacheChatToolData(sessionId, toolList, functionList);

        JSONObject obj = new JSONObject();
        obj.set("model", model.getName());
        obj.set("agentId", localAgent.getId());
        obj.set("sessionId", sessionId);
        obj.set("userId", LoginContext.currentUserId());
        obj.set("debugFlag", 0);
        RedisUtil.setValue(String.format(CacheKey.SESSION_INFO, sessionId), obj, 1L, TimeUnit.HOURS);

        return sessionId;
    }

    private String initSession(Agent agent, Integer debugFlag) {
        if (StrUtil.isEmpty(agent.getLlmModelId())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }
        LlmModel model = modelService.getModelById(agent.getLlmModelId(), agent.getUserId());

        List<ToolFunction> functionList = null;
        List<ToolProvider> toolList = null;
        if (ObjectUtil.isNotEmpty(agent.getToolIds())) {
            toolList = toolService.findByIds(agent.getToolIds());
            functionList = toolService.getFunctionList(agent.getToolIds());
        }

        chatService.checkChatData(agent, model, toolList);

        String sessionId = AiClientManager.initSession(model, functionList, agent.getPrompt(),
                agent.getTemperature(), agent.getTopP(), agent.getMaxTokens());

        chatService.cacheChatToolData(sessionId, toolList, functionList);

        JSONObject obj = new JSONObject();
        obj.set("model", model.getName());
        obj.set("agentId", agent.getId());
        obj.set("userId", LoginContext.currentUserId());
        obj.set("sessionId", sessionId);
        obj.set("debugFlag", debugFlag);
        RedisUtil.setValue(String.format(CacheKey.SESSION_INFO, sessionId), obj, 1L, TimeUnit.HOURS);

        return sessionId;
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
        SseEmitter sseEmitter = new SseEmitter(1000L * 60 * 3);
        SseClientMessageHandler messageHandler = new SseClientMessageHandler(sseEmitter);
        AiClientManager.chat(sessionId, dto, messageHandler);

        return sseEmitter;
    }

    /**
     * 清空会话信息
     * 退出agent界面时、清空上下文时调用
     *
     * @param sessionId
     * @return
     */
    @PostMapping("/clearSession")
    public ResponseData<String> clearSession(@RequestParam("sessionId") String sessionId) {
        AiClientManager.clearSession(sessionId);
        return ResponseData.success();
    }

    /**
     * 清空调试记录
     *
     * @param agentId
     * @return
     */
    @PostMapping("/clearDebugRecord")
    public ResponseData<String> clearDebugRecord(@RequestParam("agentId") String agentId) {
        chatService.clearChatData(agentId);
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
    public ResponseData<List<AgentChatMessage.TaskMessage>> agentChat(@PathVariable("agentId") String agentId,
                                                                      @RequestParam(value = "debugFlag", defaultValue = "0") Integer debugFlag) {
        List<AgentChatMessage.TaskMessage> list = chatService.agentChat(agentId, debugFlag);
        return ResponseData.success(list);
    }
}
