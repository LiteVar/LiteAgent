package com.litevar.dingtalk.adapter.core.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.litevar.dingtalk.adapter.core.bot.CardManager;
import com.litevar.dingtalk.adapter.core.bot.LiteAgentManager;
import com.litevar.dingtalk.adapter.core.bot.RobotMsgCardCallbackConsumer;
import com.litevar.dingtalk.adapter.core.entity.AgentRobotRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.dingtalk.open.app.api.callback.DingTalkStreamTopics.BOT_MESSAGE_TOPIC;
import static com.litevar.dingtalk.adapter.common.core.constant.CacheConstants.LITE_AGENT_CHAT_SESSION_KEY;

/**
 *
 * agent和钉钉机器人注册器
 *
 * @author Teoan
 * @since 2025/8/15 14:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRobotRegister {

    private final RedissonClient redissonClient;
    private final HashMap<String, OpenDingTalkClient> agentDingTalkClientMap = new HashMap<>(8);

    /**
     * 注册机器人和agent的绑定关系
     *
     * @param agentRobotRef 机器人和agent的绑定关系
     */
    public void register(AgentRobotRef agentRobotRef) {
        RMapCache<String, String> mapCache = redissonClient.getMapCache(StrUtil.format(LITE_AGENT_CHAT_SESSION_KEY, agentRobotRef.getRobotCode()));
        OpenDingTalkClient client = OpenDingTalkStreamClientBuilder
                .custom()
                .credential(new AuthClientCredential(agentRobotRef.getRobotClientId(), agentRobotRef.getRobotClientSecret()))
                .registerCallbackListener(BOT_MESSAGE_TOPIC, new RobotMsgCardCallbackConsumer(
                        new LiteAgentManager(mapCache, agentRobotRef.getAgentBaseUrl(), agentRobotRef.getAgentApiKey()),
                        new CardManager(agentRobotRef.getRobotClientId(),agentRobotRef.getRobotClientSecret(), agentRobotRef.getCardTemplateId())
                ))
                .build();
        try {
            client.start();
            agentDingTalkClientMap.put(agentRobotRef.getRobotCode(), client);
        } catch (Exception e) {
            log.error("start OpenDingTalkClient error:{}", e.getMessage(), e);
        }
    }

    /**
     * 注销机器人和agent的绑定关系
     *
     * @param robotCode 机器人code
     */
    public void unRegister(String robotCode) {
        OpenDingTalkClient client = agentDingTalkClientMap.get(robotCode);
        if (ObjUtil.isNotEmpty(client)) {
            try {
                // 注销的时候清除liteagent的对话session缓存
                RMapCache<String, String> mapCache = redissonClient.getMapCache(StrUtil.format(LITE_AGENT_CHAT_SESSION_KEY, robotCode));
                mapCache.clear();
                client.stop();
                agentDingTalkClientMap.remove(robotCode);
            } catch (Exception e) {
                log.error("stop OpenDingTalkClient error:{}", e.getMessage(), e);
            }
        }
    }


}
