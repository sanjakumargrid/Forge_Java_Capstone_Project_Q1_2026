package com.talentgrid.demand.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO projection representing an Account from the User Auth Service.
 * Used exclusively for denormalizing the account name during demand creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private Long id;
    private String name;
}
