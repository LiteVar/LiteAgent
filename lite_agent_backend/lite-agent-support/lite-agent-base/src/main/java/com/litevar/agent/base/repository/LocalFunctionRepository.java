package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.LocalFunction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author uncle
 * @since 2024/11/18 11:01
 */
public interface LocalFunctionRepository extends MongoRepository<LocalFunction, String>, QuerydslPredicateExecutor<LocalFunction> {
}