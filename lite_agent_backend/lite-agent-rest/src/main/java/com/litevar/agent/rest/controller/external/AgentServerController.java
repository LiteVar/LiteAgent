package com.litevar.agent.rest.controller.external;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.dto.ExternalSendMsgDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.agent.ChatService;
import com.litevar.agent.core.module.tool.executor.OpenToolThirdExecutor;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.openai.agent.AgentMsgType;
import com.litevar.agent.rest.openai.agent.MultiAgent;
import com.litevar.agent.rest.openai.handler.ExternalApiMessageHandler;
import com.litevar.agent.rest.openai.message.UserSendMessage;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.util.AgentUtil;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

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
    private AgentUtil agentUtil;
    @Autowired
    private OpenToolThirdExecutor openToolThirdExecutor;
    @Autowired
    private ChatService chatService;
    @Autowired
    private Scheduler customScheduler;

    @IgnoreAuth
    @PostMapping("/initSession")
    public Object initSession(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        String agentId = agentUtil.getAgentIdFromToken(token);
        Agent agent = agentService.findById(agentId);

        List<String> datasetIds = agentDatasetRelaService.listDatasets(agentId).parallelStream().map(Dataset::getId).toList();
        String sessionId = AgentManager.initSession(agent, datasetIds, 0, agent.getUserId(), AgentCallType.EXTERNAL.getCallType());
        return Dict.create().set("sessionId", sessionId);
    }

    @IgnoreAuth
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                                              @RequestParam("sessionId") String sessionId,
                                              @RequestBody @Valid ExternalSendMsgDTO dto) {
        return Flux.<ServerSentEvent<String>>create(sink -> {
                    String agentId = agentUtil.getAgentIdFromToken(token);
                    String requestId = IdUtil.getSnowflakeNextIdStr();
                    boolean isStream = dto.getIsChunk();

                    List<AgentPlanningDTO> taskList = new ArrayList<>();

                    String taskId = requestId;
                    ExternalApiMessageHandler handler = new ExternalApiMessageHandler(agentId, sessionId, sink, isStream, requestId);

                    try {
                        Optional<ExternalSendMsgDTO.Content> opt = dto.getContent().stream().filter(i -> StrUtil.equals(i.getType(), "execute")).findFirst();

                        if (opt.isPresent()) {
                            String planId = opt.get().getMessage();
                            List<AgentPlanningDTO> cacheTaskList = (List<AgentPlanningDTO>) RedisUtil.getValue(String.format(CacheKey.SESSION_PLAN_INFO, planId));
                            if (cacheTaskList == null) {
                                sink.error(new StreamException(404, "找不到该规划信息"));
                                return;
                            }
                            taskList.addAll(cacheTaskList);

                            RedisUtil.delKey(String.format(CacheKey.SESSION_PLAN_INFO, planId));
                        }
                        AgentManager.getHandler(sessionId).add(handler);
                    } catch (ServiceException ex) {
                        sink.error(new StreamException(ex.getCode(), ex.getMessage()));
                        return;
                    } catch (Exception ex) {
                        sink.error(new StreamException(500, ex.getMessage()));
                        return;
                    }

                    sink.onCancel(() -> {
                        log.info("SSE连接被取消: sessionId={}, requestId={}", sessionId, requestId);
                        afterChat(sessionId, requestId);
                    });

                    // 响应式异步处理
                    Mono.fromRunnable(() -> {
                        MultiAgent agent = AgentManager.getAgent(sessionId);

                        CurrentAgentRequest.AgentRequest currentContext = new CurrentAgentRequest.AgentRequest();
                        currentContext.setSessionId(sessionId);
                        currentContext.setParentTaskId(null);
                        currentContext.setTaskId(null);
                        currentContext.setRequestId(requestId);
                        currentContext.setAgentId(agent.getAgentId());
                        CurrentAgentRequest.setContext(currentContext);

                        if (!taskList.isEmpty()) {
                            currentContext.setTaskId(taskId);
                            AgentSendMsgDTO msg = new AgentSendMsgDTO();
                            msg.setMessage("执行方案");
                            msg.setType("text");
                            UserSendMessage userSendMessage = new UserSendMessage(sessionId, taskId, agent.getAgentId(), List.of(msg), requestId);
                            AgentManager.handleMsg(AgentMsgType.USER_SEND_MSG, userSendMessage);

                            List<MultiAgent> taskAgentList = agentUtil.createAgent(taskList);
                            String result = agentUtil.executeAgent(taskAgentList, isStream);
                            agentUtil.summary(result, agent, isStream);
                        } else {
                            List<AgentSendMsgDTO> msg = BeanUtil.copyToList(dto.getContent(), AgentSendMsgDTO.class);
                            UserSendMessage userSendMessage = new UserSendMessage(sessionId, taskId, agent.getAgentId(), msg, requestId);
                            AgentManager.handleMsg(AgentMsgType.USER_SEND_MSG, userSendMessage);

                            List<Message> submitMsg = new ArrayList<>();
                            for (ExternalSendMsgDTO.Content m : dto.getContent()) {
                                submitMsg.add(UserMessage.of(m.getMessage()));
                            }
                            AgentManager.chat(agent, taskId, submitMsg, isStream);
                        }

                        afterChat(sessionId, requestId);
                    }).subscribeOn(customScheduler).subscribe(null, sink::error, () -> afterChat(sessionId, requestId));
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
            if (h instanceof ExternalApiMessageHandler handler && StrUtil.equals(handler.getRequestId(), requestId)) {
                handler.disconnect(requestId);
                return true;
            }
            return false;
        });
    }

    @IgnoreAuth
    @GetMapping("/version")
    public Object version(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        agentUtil.getAgentIdFromToken(token);
        return Dict.create().set("version", "2.0.0");
    }

    @IgnoreAuth
    @GetMapping("/clear")
    public Object clear(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                        @RequestParam("sessionId") String sessionId) {
        agentUtil.getAgentIdFromToken(token);
        AgentManager.clearSession(sessionId);
        return Dict.create().set("id", sessionId);
    }

    @IgnoreAuth
    @PostMapping("/callback")
    public Object callback(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                           @RequestParam("sessionId") String sessionId,
                           @RequestBody @Valid CallbackParam data) {
        agentUtil.getAgentIdFromToken(token);
        AgentManager.getAgent(sessionId);
        String callId = data.getId();
        String result = JSONUtil.toJsonStr(data.getResult());
        log.info("收到第三方系统接口回调结果,callId:{},result:{}", callId, result);
        openToolThirdExecutor.callback(callId, result);

        return Dict.create().set("result", "success");
    }

    @IgnoreAuth
    @GetMapping("/history")
    public Object history(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                          @RequestParam("sessionId") String sessionId) {
        String agentId = agentUtil.getAgentIdFromToken(token);
        return chatService.agentSessionChat(agentId, sessionId);
    }

    @IgnoreAuth
    @GetMapping("/stop")
    public Object stop(@RequestHeader(CommonConstant.HEADER_AUTH) String token,
                       @RequestParam(value = "sessionId") String sessionId,
                       @RequestParam(value = "taskId", required = false) String taskId) {
        String agentId = agentUtil.getAgentIdFromToken(token);
        return Dict.create().set("failure", "该功能暂未实现");
    }

    @IgnoreAuth
    @GetMapping("/agentInfo")
    public Object agentInfo(@RequestHeader(CommonConstant.HEADER_AUTH) String token) {
        String agentId = agentUtil.getAgentIdFromToken(token);
        return agentService.apiAgentDetail(agentId);
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
}
