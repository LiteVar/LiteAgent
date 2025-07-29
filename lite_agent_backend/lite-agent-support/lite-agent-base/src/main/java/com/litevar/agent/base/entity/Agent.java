package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * agents 数据表
 *
 * @author reid
 * @since 2024/8/8
 */
@Data
@CollectionName("agent")
public class Agent {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 创建者id
     */
    private String userId;
    /**
     * 工作空间id
     */
    private String workspaceId;

    /**
     * agent名称
     */
    private String name;
    /**
     * 图标url
     */
    private String icon = "";
    /**
     * 描述,可空
     */
    private String description = "";
    /**
     * 提示词
     */
    private String prompt = "";
    /**
     * 关联的大模型id
     */
    private String llmModelId = "";

    /**
     * 方法列表
     */
    private List<AgentFunction> functionList;
    /**
     * 方法执行顺序(functionId)
     */
    private List<String> sequence;

    /**
     * agent状态: 0-初始待发布,1-已发布生效
     */
    private Integer status = 0;

    /**
     * 温度值
     */
    private Double temperature;

    /**
     * 概率抽样的 p 值
     */
    private Double topP;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    /**
     * 子agent(普通,分发,分思) id
     */
    private List<String> subAgentIds;

    /**
     * agent类型
     *
     * @see com.litevar.agent.base.enums.AgentType
     */
    private Integer type = 0;
    /**
     * 执行模式
     *
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer mode = 0;

    /**
     * true-auto agent,false-非auto agent
     */
    private Boolean autoAgentFlag = Boolean.FALSE;

    /**
     * tts模型id
     */
    private String ttsModelId;

    /**
     * asr模型id
     */
    private String asrModelId;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";

    @Data
    public static class AgentFunction {
        private String functionId;
        /**
         * 执行模式
         *
         * @see com.litevar.agent.base.enums.ExecuteMode
         */
        private Integer mode;
    }
}
