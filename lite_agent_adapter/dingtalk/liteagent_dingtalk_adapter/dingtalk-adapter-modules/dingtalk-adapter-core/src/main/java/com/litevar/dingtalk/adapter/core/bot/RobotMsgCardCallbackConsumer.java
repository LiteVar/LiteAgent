package com.litevar.dingtalk.adapter.core.bot;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.litevar.dingtalk.adapter.common.core.exception.ServiceException;
import com.litevar.dingtalk.adapter.core.service.IRobotPermissionsService;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.litevar.dingtalk.adapter.common.core.constant.LiteAgentConstants.LITE_AGENT_EXCEPTION_FLAG;


/**
 * 对接钉钉机器人(ai卡片消息)
 *
 * @author Teoan
 * @since 2025/8/14 12:18
 */

@Slf4j
@AllArgsConstructor
public class RobotMsgCardCallbackConsumer implements OpenDingTalkCallbackListener<JSONObject, JSONObject> {


    private LiteAgentManager liteAgentManager;

    private CardManager cardManager;

    /**
     * 卡片内容更新间隔
     */
    private final static Integer STREAM_UPDATE_LENGTH = 300;

    /**
     * 一次对话卡片内容最多更新次数
     */
    private final static Integer MAX_STREAM_UPDATE_COUNT = 6;


    @Override
    public JSONObject execute(JSONObject chatbotMessage) {
        String content = chatbotMessage.getJSONObject("text").getStr("content");
        String robotCode = chatbotMessage.getStr("robotCode");
        String conversationType = chatbotMessage.getStr("conversationType");
        String conversationId = chatbotMessage.getStr("conversationId");
        String senderStaffId = chatbotMessage.getStr("senderStaffId");
        String senderNick = chatbotMessage.getStr("senderNick");
        String openConvId = StrUtil.equals(conversationType, "1") ? senderStaffId : conversationId;
        log.debug("receive message by robot:{}", chatbotMessage);
        // 这是开发者自定义的，后续对卡片的投放和互动操作，均是通过 outTrackId 来完成
        String outTrackId = UUID.randomUUID().toString(true);

        cardManager.sendCard(outTrackId, robotCode, openConvId, conversationType, Map.of(senderStaffId, senderNick));
        // 判断权限
        IRobotPermissionsService permissionsService = SpringUtil.getBean(IRobotPermissionsService.class);
        if (!permissionsService.checkRobotPermission(robotCode, senderStaffId)) {
            cardManager.finishAiCard(outTrackId, "权限不足，请联系管理员！");
            return null;
        }

        ThreadUtil.execute(() -> {
            try {
                // 思考内容
                StringBuilder reasoningFullContent = new StringBuilder();
                StringBuilder fullContent = new StringBuilder();
                final AtomicInteger contentLen = new AtomicInteger();
                final AtomicInteger reasoningContentLen = new AtomicInteger();
                final AtomicInteger sendCount = new AtomicInteger(0);

                liteAgentManager.sendLiteAgentMessage(content, conversationId, true, new MessageHandler() {
                    @Override
                    public void handleMessage(ApiRecords.AgentMessage agentMessage) {
                        log.debug("receive handleMessage from liteAgent:{}", agentMessage);
                        if (agentMessage.getType().equals(ApiRecords.MessageType.taskStatus) &&
                                ObjUtil.isNotEmpty(agentMessage.getContent())) {
                            JSONObject jsonObject = JSONUtil.parseObj(agentMessage.getContent(),
                                    JSONConfig.create().setIgnoreError(true));
                            if (LITE_AGENT_EXCEPTION_FLAG.equals(jsonObject.getStr("status"))) {
                                liteAgentManager.cleanSession(conversationId);
                                throw new ServiceException("大模型异常,暂时无法连接到服务器,请稍后再试!");
                            }
                        }
                    }

                    @Override
                    public void handleChunk(ApiRecords.AgentMessage agentMessage) {
                        boolean shouldUpdate = false;
                        if (StrUtil.isNotEmpty(agentMessage.getPart())) {
                            if (agentMessage.getType().equals(ApiRecords.MessageType.reasoningContent)) {
                                reasoningFullContent.append(agentMessage.getPart());
                                String content = reasoningFullContent.toString();
                                int fullContentLen = content.length();
                                if (fullContentLen - reasoningContentLen.get() > STREAM_UPDATE_LENGTH) {
                                    shouldUpdate = true;
                                    reasoningContentLen.set(fullContentLen);
                                }
                            } else {
                                fullContent.append(agentMessage.getPart());
                                String content = fullContent.toString();
                                int fullContentLen = content.length();
                                if (fullContentLen - contentLen.get() > STREAM_UPDATE_LENGTH) {
                                    shouldUpdate = true;
                                    contentLen.set(fullContentLen);
                                }
                            }
                            // 统一更新卡片内容，包含思考和输出
                            if (shouldUpdate && sendCount.get() < MAX_STREAM_UPDATE_COUNT - 1) {
                                sendCount.incrementAndGet();
                                String combinedContent = reasoningFullContent + fullContent.toString();
                                cardManager.streamUpdate(outTrackId, combinedContent, false, "content");
                            }
                        }


                    }

                    @Override
                    public void handleFunctionCall(ApiRecords.AgentMessage agentMessage) {

                    }

                    @Override
                    public void handlePlanningMessage(ApiRecords.AgentMessage agentMessage) {

                    }
                }, () -> {
                    String combinedContent = formatCombinedContent(
                            reasoningFullContent.toString(),
                            fullContent.toString()
                    );
                    sendCount.incrementAndGet();
                    log.debug("agent steam message send end fullContent:{},sendStreamUpdateCount:{}", combinedContent,sendCount);
                    // 流式响应结束时
                    cardManager.finishAiCard(outTrackId, combinedContent);
                });
            } catch (Exception e) {
                // 异常ai卡片处理
                cardManager.finishAiCard(outTrackId, e.getMessage());
                log.error("error when send message to liteAgent", e);
            }
        });


        return null;
    }


    /**
     * 同时显示思考内容和输出内容的格式化方法
     */
    private String formatCombinedContent(String thinkingContent, String outputContent) {
        // 如果没有思考内容，则直接返回输出内容
        if (StrUtil.isBlank(thinkingContent)) {
            return outputContent;
        }
        String contentTemplate = """
                > {}
                
                
                {}
                """;
        return StrUtil.format(contentTemplate, thinkingContent.replaceAll("\n", "\n> "), outputContent);
    }
}
