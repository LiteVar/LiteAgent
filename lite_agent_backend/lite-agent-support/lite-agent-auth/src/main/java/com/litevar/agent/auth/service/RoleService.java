package com.litevar.agent.auth.service;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author uncle
 * @since 2024/8/5 10:51
 */
@Service
public class RoleService {
    @Autowired
    private BaseMapper baseMapper;

    @Cacheable(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId", unless = "#result == null")
    public Integer getRoles(String workspaceId, String userId) {
        WorkspaceMember member = baseMapper.one(new QueryWrapper<WorkspaceMember>().lambdaQuery()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, userId), WorkspaceMember.class);
        if (member != null) {
            return member.getRole();
        }
        return null;
    }
}