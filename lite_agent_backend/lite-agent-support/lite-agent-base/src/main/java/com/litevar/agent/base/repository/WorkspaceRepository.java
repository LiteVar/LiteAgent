package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.Workspace;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author uncle
 * @since 2024/8/1 14:56
 */
public interface WorkspaceRepository extends MongoRepository<Workspace, String> {

    Workspace findByName(String name);
}