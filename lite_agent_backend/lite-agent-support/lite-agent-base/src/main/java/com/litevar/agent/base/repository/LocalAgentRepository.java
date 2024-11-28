package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.LocalAgent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

/**
 * @author uncle
 * @since 2024/11/14 10:43
 */
public interface LocalAgentRepository extends MongoRepository<LocalAgent, String>, QuerydslPredicateExecutor<LocalAgent> {

    List<LocalAgent> findByUserId(String userId);
}