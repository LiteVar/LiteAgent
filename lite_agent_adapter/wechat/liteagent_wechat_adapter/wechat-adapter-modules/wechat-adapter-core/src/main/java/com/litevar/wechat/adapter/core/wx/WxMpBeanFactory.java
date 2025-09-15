package com.litevar.wechat.adapter.core.wx;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.litevar.wechat.adapter.common.core.exception.ServiceException;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.litevar.wechat.adapter.core.service.IAgentWxService;
import com.litevar.wechat.adapter.core.wx.handler.WxMpTextMessageHandlerImpl;
import me.chanjar.weixin.common.util.http.apache.ApacheHttpClientBuilder;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceHttpClientImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;

/**
 * 微信公众号服务工厂类
 *
 * @author Teoan
 * @since 2025/8/28 18:12
 */
public class WxMpBeanFactory {


    public static WxMpService getWxMpService(String appId) {
        IAgentWxService agentWxService = SpringUtil.getBean(IAgentWxService.class);
        AgentWxRefDTO agentWxRef = agentWxService.getAgentWxRefByAppId(appId);
        if (ObjUtil.isEmpty(agentWxRef)) {
            throw new ServiceException("未找到该公众号配置appId:" + appId);
        }

        WxMpServiceHttpClientImpl wxMpService = new WxMpServiceHttpClientImpl();

        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        setWxMpInfo(config,agentWxRef);

        wxMpService.setWxMpConfigStorage(config);
        return wxMpService;
    }


    public static WxMpService getWxMpService(AgentWxRefDTO agentWxRef) {
        WxMpServiceHttpClientImpl wxMpService = new WxMpServiceHttpClientImpl();

        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        setWxMpInfo(config,agentWxRef);

        wxMpService.setWxMpConfigStorage(config);
        return wxMpService;
    }


    public static WxMpMessageRouter getWxMpMessageRouter(WxMpService wxMpService){
        WxMpTextMessageHandlerImpl wxMpTextMessageHandler = SpringUtil.getBean(WxMpTextMessageHandlerImpl.class);
        // 目前仅处理文本消息
        return new WxMpMessageRouter(wxMpService)
                .rule()
                .msgType("text")
                .handler(wxMpTextMessageHandler)
                .async(false)
                .end()
                .rule()
                .msgType("event")
                .event("debug_demo")
                .handler(wxMpTextMessageHandler)
                .async(false)
                .end()
                .rule()
                // 另外一个匹配规则
                .end();
    }


    private static void setWxMpInfo(WxMpDefaultConfigImpl config,AgentWxRefDTO agentWxRef) {
        config.setAppId(agentWxRef.getAppId());
        config.setSecret(agentWxRef.getAppSecret());
        config.setToken(agentWxRef.getToken());
        config.setAesKey(agentWxRef.getAesKey());
        config.setApacheHttpClientBuilder(DefaultApacheHttpClientBuilder.get());
    }

}
