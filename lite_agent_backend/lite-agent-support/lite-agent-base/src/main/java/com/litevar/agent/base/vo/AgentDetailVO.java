package com.litevar.agent.base.vo;

import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.AgentApiKey;
import com.litevar.agent.base.entity.LlmModel;
import lombok.Data;

import java.util.List;

/**
 * @author reid
 * @since 2024/8/12
 */
@Data
public class AgentDetailVO {
    private Agent agent;
    private LlmModel model;

    /**
     * 方法列表
     */
    private List<FunctionVO> functionList;

    /**
     * api key
     */
    private List<AgentApiKey> apiKeyList;

    /**
     * 数据集列表
     */
    private List<DatasetVO> datasetList;

    /**
     * 是否能编辑
     */
    private Boolean canEdit = Boolean.FALSE;

    /**
     * 是否能删除
     */
    private Boolean canDelete = Boolean.FALSE;

    /**
     * 是否能发布
     */
    private Boolean canRelease = Boolean.FALSE;
}
