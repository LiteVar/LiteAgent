package com.litevar.agent.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间成员列表信息
 *
 * @author uncle
 * @since 2024/8/7 14:47
 */
@Data
public class WorkspaceMemberVO {
    /**
     * 成员id
     */
    private String id;
    /**
     * 空间id
     */
    private String workspaceId;
    /**
     * 成员用户id
     */
    private String userId;

    /**
     * 昵称
     */
    private String name;

    /**
     * 成员账号
     */
    private String email;
    /**
     * 空间成员角色
     */
    private Integer role;
    /**
     * 加入时间
     */
    private LocalDateTime createTime;
}
