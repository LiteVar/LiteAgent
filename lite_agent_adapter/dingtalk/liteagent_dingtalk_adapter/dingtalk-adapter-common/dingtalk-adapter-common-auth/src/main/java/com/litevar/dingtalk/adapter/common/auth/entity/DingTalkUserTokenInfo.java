package com.litevar.dingtalk.adapter.common.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token信息
 * @author Teoan
 * @since 2025/8/12 14:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DingTalkUserTokenInfo {

    public String accessToken;

    public String corpId;

    public Long expireIn;

    public String refreshToken;
}
