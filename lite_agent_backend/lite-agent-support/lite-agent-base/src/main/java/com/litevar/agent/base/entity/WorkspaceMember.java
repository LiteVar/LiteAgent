package com.litevar.agent.base.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 工作空间成员
 *
 * @author uncle
 * @since 2024/8/1 11:58
 */
@Data
@Document("workspace_member")
public class WorkspaceMember {

    private String id;
    /**
     * 空间id
     */
    @Indexed
    private String workspaceId;
    /**
     * 成员用户id
     */
    @Indexed
    private String userId;

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
    @CreatedDate
    private LocalDateTime createTime;
}