package com.litevar.dingtalk.adapter.core.bot;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.dingtalkcard_1_0.Client;
import com.aliyun.dingtalkcard_1_0.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import com.litevar.dingtalk.adapter.common.auth.utils.DingTalkAppTokenUtil;
import lombok.extern.slf4j.Slf4j;
import shade.com.alibaba.fastjson2.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息卡片处理器
 *
 * @author Teoan
 * @since 2025/8/18 15:27
 */
@Slf4j
public class CardManager {

    private final Client client;

    private final String clientId;

    private final String clientSecret;

    private final String cardTemplateId;

    private final DingTalkAppTokenUtil dingTalkAppTokenUtil;


    public CardManager(String clientId, String clientSecret, String cardTemplateId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cardTemplateId = cardTemplateId;
        this.client = createClient();
        this.dingTalkAppTokenUtil = SpringUtil.getBean(DingTalkAppTokenUtil.class);
    }

    public void sendCard(String outTrackId, String robotCode, String openConvId,
                         String conversationType, Map<String, String> atUserMap) {
        try {
            CreateAndDeliverHeaders headers
                    = new CreateAndDeliverHeaders();
            headers.xAcsDingtalkAccessToken = dingTalkAppTokenUtil.getDingTalkAppToken(clientId, clientSecret);

            Map<String, String> cardDataMap = new HashMap<>();
            cardDataMap.put("title", "AI助理回复中");
            // 构造 cardData
            CreateAndDeliverRequest.CreateAndDeliverRequestCardData cardData =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestCardData();
            cardData.setCardParamMap(cardDataMap);

            // 构造 Model
            CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenDeliverModel imGroupOpenDeliverModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenDeliverModel()
                            .setRobotCode(robotCode);

            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel imRobotOpenDeliverModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenDeliverModel()
                            .setRobotCode(robotCode).setSpaceType("IM_ROBOT");
            Map<String, String> lastMsgI18n = new HashMap<>();
            CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenSpaceModel imGroupOpenSpaceModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImGroupOpenSpaceModel()
                            .setLastMessageI18n(lastMsgI18n)
                            .setSupportForward(true);
            CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel imRobotOpenSpaceModel =
                    new CreateAndDeliverRequest.CreateAndDeliverRequestImRobotOpenSpaceModel()
                            .setLastMessageI18n(lastMsgI18n)
                            .setSupportForward(true);
            // 2：群聊, 1：单聊
            CreateAndDeliverRequest request
                    = new CreateAndDeliverRequest()
                    .setOutTrackId(outTrackId)
                    .setCardTemplateId(cardTemplateId)
                    .setCardData(cardData)
                    .setCallbackType("STREAM")
                    .setUserIdType(1);

            if (StrUtil.equals(conversationType, "2")) {
                // 群里场景at对应的人
                request.setCardAtUserIds(atUserMap.keySet().stream().toList());
                cardDataMap.put("atContent", getAtUserContent(atUserMap));
                request.setImGroupOpenDeliverModel(imGroupOpenDeliverModel)
                        .setImGroupOpenSpaceModel(imGroupOpenSpaceModel)
                        .setOpenSpaceId(getGroupOpenSpaceId(openConvId));
            } else if (StrUtil.equals(conversationType, "1")) {
                request.setImRobotOpenDeliverModel(imRobotOpenDeliverModel)
                        .setImRobotOpenSpaceModel(imRobotOpenSpaceModel)
                        .setOpenSpaceId(getRobotOpenSpaceId(openConvId));
            }


            CreateAndDeliverResponse resp = client.createAndDeliverWithOptions(request, headers,
                    new RuntimeOptions());
            log.debug("CardManager#sendCard get resp:{}", JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("CardManager#sendCard get exception, msg:{}", e.getMessage());
        }
    }

    /**
     * 更新卡片
     *
     * @param outTrackId
     * @param cardDataMap 绑定的变量
     */
    public void updateCard(String outTrackId, Map<String, String> cardDataMap) {
        UpdateCardHeaders updateCardHeaders
                = new UpdateCardHeaders();
        updateCardHeaders.xAcsDingtalkAccessToken = dingTalkAppTokenUtil.getDingTalkAppToken(clientId, clientSecret);;
        UpdateCardRequest.UpdateCardRequestCardUpdateOptions cardUpdateOptions
                = new UpdateCardRequest.UpdateCardRequestCardUpdateOptions()
                .setUpdateCardDataByKey(true);
        UpdateCardRequest.UpdateCardRequestCardData cardData
                = new UpdateCardRequest.UpdateCardRequestCardData()
                .setCardParamMap(cardDataMap);
        UpdateCardRequest updateCardRequest
                = new UpdateCardRequest()
                .setOutTrackId(outTrackId)
                .setCardData(cardData)
                .setCardUpdateOptions(cardUpdateOptions)
                .setUserIdType(1);
        try {
            client.updateCardWithOptions(updateCardRequest, updateCardHeaders,
                    new RuntimeOptions());
        } catch (TeaException err) {
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#updateCard get TeaException, msg:{} ", err.message);
            }

        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#updateCard get Exception, msg:{} ", err.message);
            }

        }
    }

    public void streamUpdate(String outTrackId, String content, Boolean isFinalize, String varName) {
        try {
            StreamingUpdateHeaders headers = new StreamingUpdateHeaders();
            headers.xAcsDingtalkAccessToken = dingTalkAppTokenUtil.getDingTalkAppToken(clientId, clientSecret);;
            StreamingUpdateRequest request =
                    new StreamingUpdateRequest().setOutTrackId(outTrackId).setGuid(UUID.randomUUID().toString()).setKey(
                            varName).setContent(content).setIsFull(true).setIsFinalize(isFinalize);
            client.streamingUpdateWithOptions(request, headers, new RuntimeOptions());

        } catch (Exception e) {
            TeaException err = new TeaException(e.getMessage(), e);
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#streamUpdate get exception, msg:{}", e.getMessage());
            }
        }

    }

    public void streamError(String outTrackId, String content) {
        try {
            StreamingUpdateHeaders headers = new StreamingUpdateHeaders();
            headers.xAcsDingtalkAccessToken = dingTalkAppTokenUtil.getDingTalkAppToken(clientId, clientSecret);
            StreamingUpdateRequest request =
                    new StreamingUpdateRequest().setOutTrackId(outTrackId).setGuid(UUID.randomUUID().toString()).setKey(
                            "content").setContent(content).setIsFull(true).setIsFinalize(true).setIsError(true);
            client.streamingUpdateWithOptions(request, headers, new RuntimeOptions());

        } catch (Exception e) {
            TeaException err = new TeaException(e.getMessage(), e);
            if (!Common.empty(err.code) && !Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
                log.error("CardManager#streamUpdate get exception, msg:{}", e.getMessage());
            }
        }

    }


    public void finishAiCard(String outTrackId, String content) {
        streamUpdate(outTrackId, content, true, "content");
    }


    protected Client createClient() {
        try {
            Config config = new Config();
            config.protocol = "https";
            config.regionId = "central";
            config.endpoint = "api.dingtalk.com";
            return new Client(config);
        } catch (Exception e) {
            log.error("createClient get excpetion, msg:{}", e.getMessage());
        }
        return null;
    }

    protected String getGroupOpenSpaceId(String openConvId) {
        return "dtv1.card//IM_GROUP." + openConvId;
    }


    protected String getRobotOpenSpaceId(String openConvId) {
        return "dtv1.card//IM_ROBOT." + openConvId;
    }

    protected String getAtUserContent(Map<String, String> atUserMap) {
        String atUserTemplate = """
                <a atId={}> {}<a>
                """;
        StringBuilder atUserContent = new StringBuilder();
        atUserMap.forEach((userId, userName) -> {
            atUserContent.append(StrUtil.format(atUserTemplate, userId, userName));
        });
        return atUserContent.toString();
    }


}