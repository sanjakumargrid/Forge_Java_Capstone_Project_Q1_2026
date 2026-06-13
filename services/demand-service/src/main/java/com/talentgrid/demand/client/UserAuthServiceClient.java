package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.AccountDto;
import com.talentgrid.demand.client.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for synchronous HTTP communication with the User/Auth Service.
 *
 * <p>Used primarily during demand creation and updates to fetch and denormalize
 * Account and Project names based on the provided IDs.
 */
@FeignClient(name = "user-auth-service", url = "${user-auth-service.url:http://localhost:8080}", path = "/api")
public interface UserAuthServiceClient {

    /**
     * Retrieves account details by ID.
     *
     * @param id the unique account ID
     * @return the lightweight account projection
     */
    @GetMapping("/accounts/{id}")
    AccountDto getAccountById(@PathVariable("id") Long id);

    /**
     * Retrieves project details by ID.
     *
     * @param id the unique project ID
     * @return the lightweight project projection
     */
    @GetMapping("/projects/{id}")
    ProjectDto getProjectById(@PathVariable("id") Long id);
}
