package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.LlmModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author uncle
 * @since 2024/8/9 10:07
 */
public interface LlmModelRepository extends MongoRepository<LlmModel, String>, QuerydslPredicateExecutor<LlmModel> {
}