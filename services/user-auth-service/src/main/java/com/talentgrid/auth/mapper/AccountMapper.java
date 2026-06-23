package com.talentgrid.auth.mapper;

import com.talentgrid.auth.dto.response.AccountResponse;
import com.talentgrid.auth.entity.Account;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting {@link Account} entities to response DTOs.
 */
@Component
public class AccountMapper {

    /**
     * Maps an account entity to its API response representation.
     *
     * @param account the persisted account
     * @return mapped response or {@code null} when the entity is {@code null}
     */
    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .accountManagerId(account.getAccountManagerId())
                .build();
    }
}
