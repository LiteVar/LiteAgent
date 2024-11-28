package com.litevar.agent.auth.service;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.repository.WorkspaceMemberRepository;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author uncle
 * @since 2024/8/5 10:51
 */
@Service
public class RoleService {

    @Resource
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Cacheable(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId", unless = "#result == null")
    public Integer getRoles(String workspaceId, String userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (member != null) {
            return member.getRole();
        }
        return null;
    }
}
