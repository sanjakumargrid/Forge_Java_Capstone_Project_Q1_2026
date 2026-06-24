package com.talentgrid.interview.client;

import com.talentgrid.interview.client.dto.EmployeeDto;
import com.talentgrid.interview.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * HTTP client for the user-auth-service.
 *
 * <p>Used by GoogleCalendarClient to resolve interviewer IDs to real corporate
 * email addresses so that Google Calendar invites and FreeBusy checks target
 * actual Google Workspace accounts instead of placeholder addresses.
 */
@Slf4j
@Component
public class EmployeeClient {

    private final WebClient webClient;

    public EmployeeClient(
            @Qualifier("userAuthWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    /**
     * Fetches an employee by their ID from the user-auth-service.
     *
     * @param employeeId the internal employee/interviewer ID
     * @return EmployeeDto containing the real corporate email address
     * @throws BusinessException if the employee is not found or the service is unavailable
     */
    public EmployeeDto getEmployee(Long employeeId) {
        try {
            EmployeeDto employee = webClient.get()
                    .uri("/employees/{id}", employeeId)
                    .retrieve()
                    .bodyToMono(EmployeeDto.class)
                    .block();

            log.debug("[EmployeeClient] Resolved employeeId={} → email={}", employeeId,
                    employee != null ? employee.getEmail() : "null");

            return employee;

        } catch (WebClientResponseException.NotFound ex) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Employee not found with id: " + employeeId
            );

        } catch (WebClientResponseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "user-auth-service error while fetching employee " + employeeId + ": "
                            + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {
            log.error("[EmployeeClient] Unable to connect to user-auth-service for employeeId={}: {}",
                    employeeId, ex.getMessage());
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect to user-auth-service to resolve interviewer email"
            );
        }
    }
}
