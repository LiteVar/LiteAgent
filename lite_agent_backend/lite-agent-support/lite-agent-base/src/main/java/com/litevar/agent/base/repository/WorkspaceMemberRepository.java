package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.WorkspaceMember;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

/**
 * @author uncle
 * @since 2024/8/1 15:44
 */
public interface WorkspaceMemberRepository extends MongoRepository<WorkspaceMember, String> {

    List<WorkspaceMember> findByUserId(String userId);

    List<WorkspaceMember> findByWorkspaceIdAndEmailIn(String workspaceId, Set<String> email);

    WorkspaceMember findByWorkspaceIdAndUserId(String workspaceId, String userId);

    List<WorkspaceMember> findByUserIdAndRole(String userId, Integer role);
}