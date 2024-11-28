package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.LocalTool;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author uncle
 * @since 2024/11/18 10:55
 */
public interface LocalToolRepository extends MongoRepository<LocalTool, String>, QuerydslPredicateExecutor<LocalTool> {
}