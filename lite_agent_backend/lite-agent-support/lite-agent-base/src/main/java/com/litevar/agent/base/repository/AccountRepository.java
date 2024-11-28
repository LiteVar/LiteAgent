package com.litevar.agent.base.repository;

import com.litevar.agent.base.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

/**
 * @author reid
 * @since 2024/7/25
 */

public interface AccountRepository extends MongoRepository<Account, String> {
    Account findByEmail(String email);

    List<Account> findByEmailIn(Set<String> emails);

    List<Account> findByNameLike(String username);
}
