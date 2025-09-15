package com.litevar.dingtalk.adapter.core.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.dingtalk.adapter.common.auth.utils.DingTalkAppTokenUtil;
import com.litevar.dingtalk.adapter.common.core.exception.ServiceException;
import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
import com.litevar.dingtalk.adapter.core.entity.AgentRobotRef;
import com.litevar.dingtalk.adapter.core.service.AgentRobotRegister;
import com.litevar.dingtalk.adapter.core.service.IAgentRobotService;
import com.litevar.dingtalk.adapter.core.utils.DingTalkPermissionsUtils;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Teoan
 * @since 2025/7/24 11:46
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRobotServiceImpl extends ServiceImpl<AgentRobotRef> implements IAgentRobotService {

    private final Converter converter;

    private final DingTalkAppTokenUtil dingTalkAppTokenUtil;

    private final AgentRobotRegister agentRobotRegister;


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<AgentRobotRef> agentRobotRefList = list();
        // 注册机器人和agent绑定
        agentRobotRefList.forEach(agentRobotRef -> {
            log.debug("注册机器人和agent的关联关系: {}", agentRobotRef);
            agentRobotRegister.register(agentRobotRef);
        });
    }


    /**
     * 创建机器人和agent的关联关系
     *
     * @param agentRobotRefDTO
     */
    @Override
    public void createAgentRobotRef(AgentRobotRefDTO agentRobotRefDTO) {
        AgentRobotRef one = lambdaQuery().eq(AgentRobotRef::getRobotCode, agentRobotRefDTO.getRobotCode()).one();
        if (ObjUtil.isNotEmpty(one)) {
            throw new ServiceException("机器人已关联");
        }
        checkAgentRobotRef(agentRobotRefDTO);
        AgentRobotRef robotRef = converter.convert(agentRobotRefDTO, AgentRobotRef.class);
        agentRobotRegister.register(robotRef);
        save(robotRef);
    }

    /**
     * 更新机器人和agent的关联关系
     *
     * @param agentRobotRefDTO
     */
    @Override
    public void updateAgentRobotRef(AgentRobotRefDTO agentRobotRefDTO) {
        AgentRobotRef one = getById(agentRobotRefDTO.getId());
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("机器人和agent的关联关系不存在!");
        }
        checkAgentRobotRef(agentRobotRefDTO);
        AgentRobotRef robotRef = converter.convert(agentRobotRefDTO, AgentRobotRef.class);
        // 先删后增
        agentRobotRegister.unRegister(one.getRobotCode());
        agentRobotRegister.register(robotRef);
        updateById(robotRef);
    }

    private void checkAgentRobotRef(AgentRobotRefDTO agentRobotRefDTO) {
        // 校验验钉钉 ClientId 和 ClientSecret 合法性
        String dingTalkAppToken = dingTalkAppTokenUtil.getDingTalkAppToken(agentRobotRefDTO.getRobotClientId(), agentRobotRefDTO.getRobotClientSecret());
        if (StrUtil.isBlank(dingTalkAppToken)) {
            throw new ServiceException("机器人ClientId或ClientSecret不合法");
        }
        DingTalkPermissionsUtils.checkPermissions(agentRobotRefDTO);
    }

    /**
     * 删除机器人和agent的关联关系
     *
     * @param robotCode 机器人编码
     */
    @Override
    public void deleteAgentRobotRef(String robotCode) {
        AgentRobotRef one = lambdaQuery().eq(AgentRobotRef::getRobotCode, robotCode).one();
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("机器人和agent的关联关系不存在!");
        }
        agentRobotRegister.unRegister(robotCode);
        removeById(one.getId());
    }


    /**
     * 获取机器人和agent的关联关系列表
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<AgentRobotRefDTO> getAgentRobotRefList(Integer pageNum, Integer pageSize, String search) {
        LambdaQueryChainWrapper<AgentRobotRef> wrapper = lambdaQuery().and(StrUtil.isNotBlank(search),
                wrapper1 -> wrapper1.like(AgentRobotRef::getName, search));
        return page(wrapper, pageNum, pageSize, AgentRobotRefDTO.class);
    }


    /**
     * 根据机器人编码获取机器人和agent的关联关系
     *
     * @param robotCode
     * @return
     */
    @Override
    public AgentRobotRefDTO getAgentRobotRefByRobotCode(String robotCode) {
        AgentRobotRef one = lambdaQuery().eq(AgentRobotRef::getRobotCode, robotCode).one();
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("机器人和agent的关联关系不存在,robotCode:" + robotCode);
        }
        return converter.convert(one, AgentRobotRefDTO.class);
    }
}