package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.AccountDto;
import com.talentgrid.demand.client.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-auth-service", url = "${user-auth-service.url:http://localhost:8080}", path = "/api")
public interface UserAuthServiceClient {

    @GetMapping("/accounts/{id}")
    AccountDto getAccountById(@PathVariable("id") Long id);

    @GetMapping("/projects/{id}")
    ProjectDto getProjectById(@PathVariable("id") Long id);
}
