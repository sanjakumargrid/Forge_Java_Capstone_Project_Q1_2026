package com.talentgrid.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Account response projection compatible with downstream Feign clients.
 */
@Getter
@Builder
@AllArgsConstructor
public class AccountResponse {

    private final Long id;
    private final String name;
    private final Long accountManagerId;
}
