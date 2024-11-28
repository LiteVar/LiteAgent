package com.litevar.agent.base.vo;

import lombok.Data;

/**
 * @author uncle
 * @since 2024/7/4 11:14
 */
@Data
public class LoginUser {
    String uuid;
    /**
     * 用户id
     */
    private String id;
    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;
}
