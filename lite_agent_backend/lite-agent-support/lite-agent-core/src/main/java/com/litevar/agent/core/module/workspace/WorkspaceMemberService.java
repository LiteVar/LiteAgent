package com.litevar.agent.core.module.workspace;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.WorkspaceMemberRepository;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author uncle
 * @since 2024/8/13 18:05
 */
@Service
public class WorkspaceMemberService {

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Cacheable(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId", unless = "#result == null")
    public Integer userRoleInt(String workspaceId, String userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (member == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }
        return member.getRole();
    }

    public RoleEnum userRole(String workspaceId, String userId) {
        Integer role = proxy().userRoleInt(workspaceId, userId);
        return RoleEnum.of(role);
    }

    private WorkspaceMemberService proxy() {
        return (WorkspaceMemberService) AopContext.currentProxy();
    }
}
