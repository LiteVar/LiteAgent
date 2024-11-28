package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.LocalModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author uncle
 * @since 2024/11/18 10:32
 */
public interface LocalModelRepository extends MongoRepository<LocalModel, String>, QuerydslPredicateExecutor<LocalModel> {

}