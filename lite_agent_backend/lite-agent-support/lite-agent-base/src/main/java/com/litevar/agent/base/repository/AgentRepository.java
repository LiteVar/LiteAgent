package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.Agent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

/**
 * @author reid
 * @since 2024/8/8
 */
public interface AgentRepository extends MongoRepository<Agent, String>, QuerydslPredicateExecutor<Agent> {
    List<Agent> findByLlmModelId(String llmModelId);
}
