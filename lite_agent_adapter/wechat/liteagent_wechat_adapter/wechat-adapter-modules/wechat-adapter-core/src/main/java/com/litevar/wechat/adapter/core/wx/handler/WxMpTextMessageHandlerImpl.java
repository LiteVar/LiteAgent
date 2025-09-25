package com.litevar.wechat.adapter.core.wx.handler;

import cn.hutool.core.util.StrUtil;
import com.litevar.wechat.adapter.core.agent.LiteAgentManager;
import com.litevar.wechat.adapter.core.agent.LiteAgentMessageTask;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.litevar.wechat.adapter.core.service.IAgentWxService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.litevar.wechat.adapter.common.core.constant.CacheConstants.LITE_AGENT_CHAT_SESSION_KEY;

/**
 *
 * @author Teoan
 * @since 2025/8/28 14:35
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMpTextMessageHandlerImpl implements WxMpMessageHandler {

    private final IAgentWxService agentWxService;

    private final RedissonClient redissonClient;


    @SneakyThrows
    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage,
                                    Map<String, Object> map,
                                    WxMpService wxMpService,
                                    WxSessionManager wxSessionManager) {
        log.debug("WxMpTextMessageHandler接收到微信消息：{}", wxMpXmlMessage);
        AgentWxRefDTO agentWxRefDTO = agentWxService.getAgentWxRefByAppId(wxMpService.getWxMpConfigStorage().getAppId());
        RMapCache<String, String> mapCache = redissonClient.getMapCache(StrUtil.format(LITE_AGENT_CHAT_SESSION_KEY, agentWxRefDTO.getAppId()));
        LiteAgentManager liteAgentManager = new LiteAgentManager(mapCache, agentWxRefDTO.getAgentBaseUrl(), agentWxRefDTO.getAgentApiKey());
        Boolean isError = false;
        CountDownLatch latch = new CountDownLatch(1);
        LiteAgentMessageTask liteAgentMessageTask = new LiteAgentMessageTask(wxMpXmlMessage, liteAgentManager, wxMpService, latch);
        liteAgentMessageTask.start();
        // 微信规定5秒内需回复内容，否则微信会进行重试，加上请求的耗时，这里取4秒
       if(latch.await(4, TimeUnit.SECONDS)){
           String replyContent = liteAgentMessageTask.getReplyContent();
           if (StrUtil.isNotBlank(replyContent)) {
               return createReply(wxMpXmlMessage, replyContent);
           }
       }else {
           // liteagent 4.5秒内是否有输出内容
           liteAgentMessageTask.setIsWxReply(true);
           isError = liteAgentMessageTask.getIsError();
       }
        return isError ? null : createReply(wxMpXmlMessage, "思考中...");
    }

    private WxMpXmlOutMessage createReply(WxMpXmlMessage wxMessage, String content) {
        return WxMpXmlOutMessage.TEXT()
                .content(content)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }

}
