package com.litevar.agent.base.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 账号表
 *
 * @author reid
 * @since 2024/7/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "account")
public class Account {
    @Id
    private String id;

    /**
     * 昵称
     */
    private String name;

    /**
     * 邮箱
     */
    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;
    @JsonIgnore
    private String salt;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 账号状态
     */
    private Integer status;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

}
