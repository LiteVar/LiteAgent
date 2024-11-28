package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.ToolFunction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

/**
 * @author uncle
 * @since 2024/10/18 16:01
 */
public interface ToolFunctionRepository extends MongoRepository<ToolFunction, String>, QuerydslPredicateExecutor<ToolFunction> {

    List<ToolFunction> findByToolIdIn(List<String> toolId);
}