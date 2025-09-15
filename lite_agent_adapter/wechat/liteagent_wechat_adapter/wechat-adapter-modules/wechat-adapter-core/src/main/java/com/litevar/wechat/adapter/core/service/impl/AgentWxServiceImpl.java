package com.litevar.wechat.adapter.core.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.wechat.adapter.common.core.exception.ServiceException;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.litevar.wechat.adapter.core.entity.AgentWxRef;
import com.litevar.wechat.adapter.core.service.IAgentWxService;
import com.litevar.wechat.adapter.core.wx.WxMpBeanFactory;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import static com.litevar.wechat.adapter.common.core.constant.CacheConstants.LITE_AGENT_CHAT_SESSION_KEY;

/**
 * @author Teoan
 * @since 2025/7/24 11:46
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentWxServiceImpl extends ServiceImpl<AgentWxRef> implements IAgentWxService {

    private final Converter converter;


    private final RedissonClient redissonClient;


    /**
     * 创建服务号和agent的关联关系
     *
     * @param agentWxRefDTO
     */
    @Override
    public void createAgentWxRef(AgentWxRefDTO agentWxRefDTO) {
        AgentWxRef one = lambdaQuery().eq(AgentWxRef::getAppId, agentWxRefDTO.getAppId()).one();
        if (ObjUtil.isNotEmpty(one)) {
            throw new ServiceException("服务号已关联");
        }
        checkAgentWxRef(agentWxRefDTO);
        AgentWxRef robotRef = converter.convert(agentWxRefDTO, AgentWxRef.class);
        save(robotRef);
    }

    /**
     * 更新服务号和agent的关联关系
     *
     * @param agentWxRefDTO
     */
    @Override
    public void updateAgentWxRef(AgentWxRefDTO agentWxRefDTO) {
        AgentWxRef one = getById(agentWxRefDTO.getId());
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("服务号和agent的关联关系不存在!");
        }
        checkAgentWxRef(agentWxRefDTO);
        AgentWxRef robotRef = converter.convert(agentWxRefDTO, AgentWxRef.class);
        updateById(robotRef);
        // 更新时清除已缓存的会话
        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(StrUtil.format(LITE_AGENT_CHAT_SESSION_KEY, agentWxRefDTO.getAppId()));
        mapCache.clear();
    }

    @SneakyThrows
    private void checkAgentWxRef(AgentWxRefDTO agentWxRefDTO) {
        // 校验服务号appId 和 appSecret合法性
        WxMpService wxMpService = WxMpBeanFactory.getWxMpService(agentWxRefDTO);
        // 校验不通过会抛出异常
        wxMpService.getAccessToken();
    }

    /**
     * 删除服务号和agent的关联关系
     *
     * @param id 服务号编码
     */
    @Override
    public void deleteAgentWxRef(String id) {
        AgentWxRef one = lambdaQuery().eq(AgentWxRef::getId, id).one();
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("服务号和agent的关联关系不存在!");
        }
        removeById(one.getId());
    }


    /**
     * 获取服务号和agent的关联关系列表
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<AgentWxRefDTO> getAgentWxRefList(Integer pageNum, Integer pageSize, String search) {
        LambdaQueryChainWrapper<AgentWxRef> wrapper = lambdaQuery().or(StrUtil.isNotBlank(search),
                wrapper1 -> wrapper1.like(AgentWxRef::getName, search)
                        .or(StrUtil.isNotBlank(search),wrapper2 -> wrapper2.like(AgentWxRef::getAppId, search)
                                .or(StrUtil.isNotBlank(search),wrapper3 -> wrapper3.like(AgentWxRef::getAgentApiKey, search))));
        return page(wrapper, pageNum, pageSize, AgentWxRefDTO.class);
    }


    /**
     * 根据服务号编码获取服务号和agent的关联关系
     *
     * @param appId
     */
    @Override
    public AgentWxRefDTO getAgentWxRefByAppId(String appId) {
        AgentWxRef one = lambdaQuery().eq(AgentWxRef::getAppId, appId).one();
        if (ObjUtil.isEmpty(one)) {
            throw new ServiceException("服务号和agent的关联关系不存在,appId:" + appId);
        }
        return converter.convert(one, AgentWxRefDTO.class);
    }
}