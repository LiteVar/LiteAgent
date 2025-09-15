package com.litevar.wechat.adapter.core.service;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.litevar.wechat.adapter.core.entity.AgentWxRef;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.IService;

/**
 * @author Teoan
 * @since 2025/7/28 14:33
 */
public interface IAgentWxService extends IService<AgentWxRef> {


    /**
     * 创建服务号和agent的关联关系
     * @param agentWxRefDTO
     */
    void createAgentWxRef(AgentWxRefDTO agentWxRefDTO);


    /**
     * 更新服务号和agent的关联关系
     * @param agentWxRefDTO
     */
    void updateAgentWxRef(AgentWxRefDTO agentWxRefDTO);



    /**
     * 删除服务号和agent的关联关系
     * @param id 主键
     */
    void deleteAgentWxRef(String id);


    /**
     * 获取服务号和agent的关联关系列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageResult<AgentWxRefDTO> getAgentWxRefList(Integer pageNum, Integer pageSize,String search);


    /**
     * 根据服务号编码获取服务号和agent的关联关系
     */
    AgentWxRefDTO getAgentWxRefByAppId(String appId);

}