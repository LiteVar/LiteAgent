package com.litevar.agent.rest.agentflow.agent;

import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.ToolDispatcher;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.service.UploadFileServiceV2;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 子agent
 *
 * @author uncle
 * @since 2026/3/26 18:24
 */
@Slf4j
@Component
public class DistributeAgent {
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    @Resource
    private AgentSessionManager manager;
    @Resource
    private LitevarProperties litevarProperties;
    @Resource
    private UploadFileServiceV2 uploadFileServiceV2;
    @Resource
    private ExecutionStopManager stopManager;
    @Resource
    private Orchestrator orchestrator;

    public String distribute(AgentContext context, ToolDispatcher.FunctionCallInfo functionCallInfo) {
        if (stopManager.shouldStop(context.getRequestId())) {
            return null;
        }
        JSONObject argObj = JSONUtil.parseObj(functionCallInfo.argument());
        List<String> imageUrls = new ArrayList<>();
        JSONArray imageUrlArray = argObj.getJSONArray("imageUrls");
        if (imageUrlArray != null) {
            imageUrlArray.forEach(item -> imageUrls.add(String.valueOf(item)));
        }
        String videoUrl = argObj.getStr("videoUrl");
        String targetAgentId = argObj.getStr("agentId");
        String cmd = argObj.getStr("cmd");

        AgentExecutionSpec agentRuntimeInfo = manager.getAgentRuntimeInfo(context.getSessionId(), targetAgentId);
        if (agentRuntimeInfo == null) {
            log.error("[sessionId={},taskId={}]agentId={},目标agent不存在", context.getSessionId(), context.getTaskId(), targetAgentId);
            return null;
        }
        String taskId = IdUtil.getSnowflakeNextIdStr();
        orchestrator.agentSwitchMessage(context, taskId, targetAgentId, agentRuntimeInfo.getAgentName());

        orchestrator.agentDistributeMessage(context, taskId, cmd, imageUrls, videoUrl, agentRuntimeInfo.getAgentId());

        List<Message> submitMsg = buildModelMsg(cmd, videoUrl, imageUrls, agentRuntimeInfo.getVision());
        var response = orchestrator.newTaskChat(context, targetAgentId, taskId, submitMsg);
        if (response != null) {
            String content = response.getChoices().get(0).getMessage().getContent();
            if (StrUtil.isNotEmpty(content)) {
                return content;
            } else {
                return "Successfully distributed command to the agent:" + agentRuntimeInfo.getAgentName();
            }
        } else {
            return "agent:" + agentRuntimeInfo.getAgentName() + " is not available";
        }
    }

    private List<Message> buildModelMsg(String cmd, String videoUrl, List<String> imageUrls, boolean supportVision) {
        boolean multiMedia = StrUtil.isNotBlank(videoUrl) || !imageUrls.isEmpty();
        List<Message> submitMsg = new ArrayList<>();
        if (supportVision) {
            List<UserMessage.ContentType> contents = new ArrayList<>();
            if (multiMedia) {
                contents.add(UserMessage.TextContentType.of(cmd));
            } else {
                submitMsg.add(UserMessage.of(cmd));
            }
            if (!imageUrls.isEmpty()) {
                imageUrls.forEach(url -> contents.add(UserMessage.ImageContentType.of(toModelMediaUrl(url))));
            }
            if (StrUtil.isNotEmpty(videoUrl)) {
                contents.add(UserMessage.VideoContentType.of(toModelMediaUrl(videoUrl)));
            }
            if (!contents.isEmpty()) {
                submitMsg.add(UserMessage.of(contents));
            }
        } else {
            submitMsg.add(UserMessage.of(cmd));
            if (!imageUrls.isEmpty()) {
                imageUrls.forEach(url -> submitMsg.add(UserMessage.of(url)));
            }
            if (StrUtil.isNotEmpty(videoUrl)) {
                submitMsg.add(UserMessage.of(videoUrl));
            }
        }

        return submitMsg;
    }

    private String toModelMediaUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        String prefix = litevarProperties.getExternalApiUrl();
        if (StrUtil.isNotBlank(contextPath)) {
            prefix += contextPath;
        }
        if (!StrUtil.startWith(url, prefix)) {
            return url;
        }
        String key;
        try {
            key = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("fileKey");
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(key)) {
                return url;
            }
        } catch (Exception e) {
            return url;
        }
        return uploadFileServiceV2.fileBase64Str(key);
    }

}
