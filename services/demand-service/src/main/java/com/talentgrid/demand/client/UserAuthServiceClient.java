package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.AccountDto;
import com.talentgrid.demand.client.dto.ManagedProjectDto;
import com.talentgrid.demand.client.dto.ProjectDto;
import com.talentgrid.demand.client.dto.UserDto;
import com.talentgrid.demand.client.dto.UserSummaryResponse;
import com.talentgrid.demand.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for synchronous HTTP communication with the User/Auth Service.
 *
 * <p>
 * Used primarily during demand creation, approval workflows,
 * notification routing, and SLA reminder processing.
 */
@FeignClient(
        name = "user-auth-service",
        url = "${user-auth-service.url:http://localhost:8080}",
        path = "/api",
        configuration = FeignConfig.class
)
public interface UserAuthServiceClient {

    /**
     * Retrieves account details by ID.
     *
     * @param id account ID
     * @return account projection
     */
    @GetMapping("/accounts/{id}")
    AccountDto getAccountById(@PathVariable("id") Long id);

    /**
     * Retrieves project details by ID.
     *
     * @param id project ID
     * @return project projection
     */
    @GetMapping("/projects/{id}")
    ProjectDto getProjectById(@PathVariable("id") Long id);

    /**
     * Projects where the authenticated user (Bearer token) is PM.
     * GET /api/projects/mine-as-pm
     */
    @GetMapping("/projects/mine-as-pm")
    List<ManagedProjectDto> getMyProjectsAsPm();

    /**
     * Retrieves user details by ID.
     *
     * @param id user ID
     * @return user projection
     */
    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    /**
     * Retrieves the active RMG responsible for a given location.
     *
     * Example:
     * GET /api/users/rmg-by-location?location=Chennai
     *
     * @param location demand location
     * @return RMG summary
     */
    @GetMapping("/users/rmg-by-location")
    UserSummaryResponse getRmgByLocation(
            @RequestParam("location") String location
    );
}