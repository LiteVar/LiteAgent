package com.litevar.wechat.adapter.core.agent;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import com.litevar.wechat.adapter.common.core.exception.ServiceException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;

import java.util.List;

import static com.litevar.wechat.adapter.common.core.constant.LiteAgentConstants.LITE_AGENT_EXCEPTION_FLAG;
import static com.litevar.wechat.adapter.common.core.constant.WechatConstants.WX_CONTENT_SIZE_OUT_OF_LIMIT;

/**
 * LiteAgent消息发送异步任务
 *
 * @author Teoan
 * @since 2025/8/29 12:02
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class LiteAgentMessageTask extends Thread {

    private WxMpXmlMessage wxMpXmlMessage;

    private LiteAgentManager liteAgentManager;

    private WxMpService wxMpService;

    private final Object lock;

    // 思考内容
    private StringBuilder reasoningFullContent = new StringBuilder();
    // 完整的内容
    private StringBuilder fullContent = new StringBuilder();
    // 断句内容
    private StringBuilder chunkContent = new StringBuilder();
    // 五秒内回复的内容
    private String replyContent = "";
    // 回复内容
    private String messageContent = "";

    // 是否已回复微信消息
    private Boolean isWxReply = false;

    // 是否发送异常
    private Boolean isError = false;

    // 发送次数 客服消息48小时内只能发5条 加上本身回复的一共6条
    private Integer sendCount = 0;

    // md水平线
    private final static List<String> horizontalList = List.of("---", "***", "___");


    public LiteAgentMessageTask(WxMpXmlMessage wxMpXmlMessage, LiteAgentManager liteAgentManager, WxMpService wxMpService, Object lock) {
        this.wxMpXmlMessage = wxMpXmlMessage;
        this.liteAgentManager = liteAgentManager;
        this.wxMpService = wxMpService;
        this.lock = lock;
    }


    /**
     * 发送LiteAgent异步消息
     */
    @Override
    public void run() {
        try {
            liteAgentManager.sendLiteAgentMessage(wxMpXmlMessage.getContent(), wxMpXmlMessage.getFromUser(), true, new MessageHandler() {
                @Override
                public void handleMessage(ApiRecords.AgentMessage agentMessage) {
                    log.debug("receive handleMessage from liteAgent:{}", agentMessage);
                    if (agentMessage.getType().equals(ApiRecords.MessageType.taskStatus) &&
                            ObjUtil.isNotEmpty(agentMessage.getContent())) {
                        JSONObject jsonObject = JSONUtil.parseObj(agentMessage.getContent(),
                                JSONConfig.create().setIgnoreError(true));
                        if (LITE_AGENT_EXCEPTION_FLAG.equals(jsonObject.getStr("status"))) {
                            liteAgentManager.cleanSession(wxMpXmlMessage.getFromUser());
                            throw new ServiceException("大模型异常，暂时无法连接到服务器，请稍后再试！");
                        }
                    }
                }

                @SneakyThrows
                @Override
                public void handleChunk(ApiRecords.AgentMessage agentMessage) {
//                    log.debug("receive handleChunk from liteAgent:{}", agentMessage);
                    if (StrUtil.isNotEmpty(agentMessage.getPart())) {
                        if (agentMessage.getType().equals(ApiRecords.MessageType.reasoningContent)) {
                            reasoningFullContent.append(agentMessage.getPart());
                        } else {
                            fullContent.append(agentMessage.getPart());
                            chunkContent.append(agentMessage.getPart());
                        }

                        String combinedContent = chunkContent.toString();
                        // 根据\n\n分段发送
                        if (StrUtil.contains(combinedContent, "\n\n") && sendCount < 5) {
                            messageContent = combinedContent.replace("\n\n", "").trim();
                            // 不发送空字符串和水平线
                            if (StrUtil.isNotEmpty(messageContent) && !horizontalList.contains(messageContent)) {
                                boolean shouldSendMessage = false;

                                // 第一次回复
                                if (sendCount.equals(0)) {
                                    replyContent = messageContent;
                                    // 如果父线程已回复过消息，此时使用微信客服消息回复用户 否则唤醒父线程进行第一次回复
                                    if (BooleanUtil.isFalse(isWxReply)) {
                                        // 唤醒父线程回复
                                        synchronized (lock) {
                                            lock.notifyAll();
                                        }
                                    } else {
                                        shouldSendMessage = true;
                                    }
                                    sendCount++;
                                } else {
                                    shouldSendMessage = true;
                                }

                                if (shouldSendMessage) {
                                    sendWxKefuMessage(messageContent);
                                    sendCount++;
                                }
                            }
                            // 发送完清空 累计下一个段落
                            chunkContent.setLength(0);

                        }

                    }
                }

                @Override
                public void handleFunctionCall(ApiRecords.AgentMessage agentMessage) {

                }

                @Override
                public void handlePlanningMessage(ApiRecords.AgentMessage agentMessage) {

                }
            }, new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    // 只有一个段落的情况
                    if (sendCount.equals(0) && BooleanUtil.isFalse(isWxReply)) {
                        // 第一个分块唤醒父线程回复
                        replyContent = chunkContent.toString();
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    } else {
                        sendWxKefuMessage(chunkContent.toString());
                    }
                    log.debug("LiteAgentManager发送消息成功，fullContent:{}", fullContent.toString());
                }
            });
        } catch (Exception e) {
            log.error("error when send message to liteAgent", e);
            // 发送错误信息给微信
            try {
                isError = true;
                sendWxKefuMessage("大模型异常，暂时无法连接到服务器，请稍后再试！");
            } catch (WxErrorException ex) {
                log.error("发送微信客服消息错误：", ex);
            }
        }
    }

    private void sendWxKefuMessage(String messageContent) throws WxErrorException {
        // 超过最大长度处理
        if (messageContent.length() > WX_CONTENT_SIZE_OUT_OF_LIMIT) {
            messageContent = StrUtil.format("{} ...", messageContent.substring(0, WX_CONTENT_SIZE_OUT_OF_LIMIT));
        }
        log.debug("\n发送微信客服消息内容：{},size:{}", messageContent, messageContent.length());
        wxMpService.getKefuService().sendKefuMessage(WxMpKefuMessage.TEXT().content(messageContent)
                .toUser(wxMpXmlMessage.getFromUser()).build());
    }


}
