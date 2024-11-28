package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.AgentChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * @author uncle
 * @since 2024/9/3 17:15
 */
public interface AgentChatMessageRepository extends MongoRepository<AgentChatMessage, String>, QuerydslPredicateExecutor<AgentChatMessage> {

    AgentChatMessage findBySessionId(String sessionId);
}