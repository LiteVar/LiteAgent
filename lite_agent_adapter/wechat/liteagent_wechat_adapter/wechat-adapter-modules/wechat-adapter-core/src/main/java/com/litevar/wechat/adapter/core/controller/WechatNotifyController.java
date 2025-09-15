package com.litevar.wechat.adapter.core.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.wechat.adapter.core.wx.WxMpBeanFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import org.springframework.web.bind.annotation.*;

import static com.litevar.wechat.adapter.common.core.constant.WechatConstants.WX_NOTIFY_FAIL;

/**
 * 接收微信消息
 *
 * @author Teoan
 * @since 2025/8/27 18:21
 */
@RestController
@RequestMapping("/api/notify")
@Tag(name = "接收微信消息", description = "接收微信消息")
@Slf4j
@RequiredArgsConstructor
public class WechatNotifyController {


    /**
     * 接收微信消息 提供给微信验证消息的确来自微信服务器
     */
    @GetMapping("/revice/{appId}")
    @Operation(summary = "接收微信消息 提供给微信验证消息的确来自微信服务器", description = "接收微信消息 提供给微信验证消息的确来自微信服务器")
    public Object receive(@PathVariable("appId") String appId,
                          @RequestParam(required = false, name = "timestamp") String timestamp,
                          @RequestParam(required = false, name = "nonce") String nonce,
                          @RequestParam(required = false, name = "signature") String signature,
                          @RequestParam(required = false, name = "echostr") String echostr) {
        log.debug("\n接收微信请求：[appId=[{}],timestamp=[{}], nonce=[{}], signature=[{}],"
                + " echostr=[{}]", appId, timestamp, nonce, signature, echostr);

        boolean flag = WxMpBeanFactory.getWxMpService(appId).checkSignature(timestamp, nonce, signature);
        if (!flag) {
            // 签名验证失败
            log.error("[wx_mp]Signature verification failed, signature: {}, timestamp: {}, nonce: {}", signature, timestamp, nonce);
            return WX_NOTIFY_FAIL;
        }

        log.debug("[wx_mp]验签通过，返回echostr: {}", echostr);
        return echostr;
    }


    /**
     * 接收微信消息 接收用户发送过来的消息
     */
    @PostMapping(value = "/revice/{appId}", produces = "application/xml;charset=UTF-8")
    @Operation(summary = "接收微信消息 接收用户发送过来的消息", description = "接收微信消息 接收用户发送过来的消息")
    public String receive(
            @PathVariable("appId") String appId,
            @RequestParam(required = false, name = "timestamp") String timestamp,
            @RequestParam(required = false, name = "nonce") String nonce,
            @RequestParam(required = false, name = "openid") String openid,
            @RequestParam(required = false, name = "signature") String signature,
            @RequestParam(required = false, name = "encrypt_type") String encryptType,
            @RequestParam(required = false, name = "msg_signature") String msgSignature,
            @RequestBody(required = false) String rawStr) {
        log.debug("\n接收微信请求：[appId=[{}], signature=[{}],openid=[{}], timestamp=[{}], nonce=[{}],encryptType=[{}],msgSignature=[{}],rawStr=[{}]"
                , appId, signature, openid, timestamp, nonce, encryptType, msgSignature, rawStr);
        WxMpService wxMpService = WxMpBeanFactory.getWxMpService(appId);
        WxMpConfigStorage wxMpConfigStorage = wxMpService.getWxMpConfigStorage();
        WxMpXmlMessage wxMpXmlMessage;
        // 微信加密模式和兼容模式encryptType都是aes，这里判断如果配置的aes为空则走明文模式
        if (StrUtil.equalsAnyIgnoreCase(encryptType, "aes") && StrUtil.isNotBlank(wxMpConfigStorage.getAesKey())) {
            wxMpXmlMessage = WxMpXmlMessage.fromEncryptedXml(rawStr, wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            WxMpXmlOutMessage outMessage = handleWxMessage(wxMpXmlMessage, wxMpService);
            return ObjUtil.isEmpty(outMessage) ? "" : outMessage.toEncryptedXml(wxMpConfigStorage);

        } else {
            wxMpXmlMessage = WxMpXmlMessage.fromXml(rawStr);
            WxMpXmlOutMessage outMessage = handleWxMessage(wxMpXmlMessage, wxMpService);
            return ObjUtil.isEmpty(outMessage) ? "" : outMessage.toXml();
        }
    }

    private WxMpXmlOutMessage handleWxMessage(WxMpXmlMessage wxMpXmlMessage, WxMpService wxMpService) {
        log.debug("\n接收微信内容：{}", wxMpXmlMessage);
        WxMpMessageRouter wxMpMessageRouter = WxMpBeanFactory.getWxMpMessageRouter(wxMpService);
        WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(wxMpXmlMessage);
        log.debug("\n发送微信回复内容：{}", outMessage);
        return outMessage;
    }

}
