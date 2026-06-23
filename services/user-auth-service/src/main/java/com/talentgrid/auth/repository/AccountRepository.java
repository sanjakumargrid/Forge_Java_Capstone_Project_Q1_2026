package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * @param name account name
     * @return whether an account with the given name already exists
     */
    boolean existsByName(String name);

    /**
     * @param name account name
     * @param id   account id to exclude from the check
     * @return whether another account already uses the given name
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * @param accountManagerId user id of the account manager
     * @return accounts managed by the given user
     */
    java.util.List<Account> findByAccountManagerId(Long accountManagerId);
}
