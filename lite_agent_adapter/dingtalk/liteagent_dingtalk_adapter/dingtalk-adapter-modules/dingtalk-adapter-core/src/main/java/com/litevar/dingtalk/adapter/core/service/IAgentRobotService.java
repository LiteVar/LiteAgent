package com.litevar.dingtalk.adapter.core.service;


import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
import com.litevar.dingtalk.adapter.core.entity.AgentRobotRef;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.IService;

/**
 * @author Teoan
 * @since 2025/7/28 14:33
 */
public interface IAgentRobotService extends IService<AgentRobotRef> {


    /**
     * 创建机器人和agent的关联关系
     * @param agentRobotRefDTO
     */
    void createAgentRobotRef(AgentRobotRefDTO agentRobotRefDTO);


    /**
     * 更新机器人和agent的关联关系
     * @param agentRobotRefDTO
     */
    void updateAgentRobotRef(AgentRobotRefDTO agentRobotRefDTO);



    /**
     * 删除机器人和agent的关联关系
     * @param robotCode 机器人编码
     */
    void deleteAgentRobotRef(String robotCode);


    /**
     * 获取机器人和agent的关联关系列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageResult<AgentRobotRefDTO> getAgentRobotRefList(Integer pageNum, Integer pageSize,String search);


    /**
     * 根据机器人编码获取机器人和agent的关联关系
     * @param robotCode
     * @return
     */
    AgentRobotRefDTO getAgentRobotRefByRobotCode(String robotCode);

}