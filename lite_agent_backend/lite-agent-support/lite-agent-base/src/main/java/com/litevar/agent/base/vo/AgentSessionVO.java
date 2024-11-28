package com.litevar.agent.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author uncle
 * @since 2024/9/10 16:41
 */
@Data
public class AgentSessionVO {

    private String agentId;

    /**
     * agent名字
     */
    private String name;

    private LocalDateTime createTime;

    /**
     * true为本地,false为云端
     */
    private Boolean localFlag;
}
