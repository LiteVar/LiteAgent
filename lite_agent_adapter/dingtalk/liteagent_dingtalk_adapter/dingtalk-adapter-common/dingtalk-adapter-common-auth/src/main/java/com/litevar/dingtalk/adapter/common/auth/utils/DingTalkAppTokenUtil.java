package com.litevar.dingtalk.adapter.common.auth.utils;

import cn.hutool.core.util.StrUtil;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.litevar.dingtalk.adapter.common.core.constant.CacheConstants.DING_TALK_APP_TOKEN_KEY;

/**
 *
 * @author Teoan
 * @since 2025/8/12 18:13
 */
@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class DingTalkAppTokenUtil {


    private final RedissonClient redissonClient;



    /**
     * 获取钉钉企业accessToken(企业内部应用)
     *
     * @return
     */
    public String getDingTalkAppToken(String clientId,String clientSecret) {
        RBucket<String> bucket = redissonClient.getBucket(StrUtil.format(DING_TALK_APP_TOKEN_KEY, clientId));
        if(StrUtil.isNotBlank(bucket.get())){
            return bucket.get();
        }

        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = new com.aliyun.dingtalkoauth2_1_0.Client(config);
            com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest getAccessTokenRequest = new com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest()
                    .setAppKey(clientId)
                    .setAppSecret(clientSecret);
            GetAccessTokenResponse accessToken = client.getAccessToken(getAccessTokenRequest);

            // 缓存
            bucket.set(accessToken.getBody().getAccessToken(), Duration.ofSeconds(accessToken.getBody().getExpireIn()));
            return accessToken.getBody().getAccessToken();
        } catch (Exception e) {
            log.error("获取钉钉企业accessToken失败,{}", e.getMessage());
//            throw new ServiceException(StrUtil.format("获取钉钉企业accessToken失败,{}", e.getMessage()));
        }
        return null;
    }

}
