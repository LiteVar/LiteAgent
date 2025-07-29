package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

/**
 * agent分享给外部调用的api信息
 *
 * @author uncle
 * @since 2025/3/21 17:58
 */
@Data
@CollectionName("agent_api_key")
public class AgentApiKey {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String agentId;

    private String apiUrl;

    private String apiKey;
}
