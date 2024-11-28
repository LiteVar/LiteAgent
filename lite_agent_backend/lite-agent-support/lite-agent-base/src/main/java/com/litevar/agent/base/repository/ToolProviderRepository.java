package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.ToolProvider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author reid
 * @since 2024/7/29
 */
public interface ToolProviderRepository extends MongoRepository<ToolProvider, String>, QuerydslPredicateExecutor<ToolProvider> {

    ToolProvider findByWorkspaceIdAndName(String workspaceId, String name);
}
