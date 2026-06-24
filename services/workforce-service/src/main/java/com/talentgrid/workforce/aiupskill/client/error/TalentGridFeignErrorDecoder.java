package com.talentgrid.workforce.aiupskill.client.error;

import com.talentgrid.workforce.aiupskill.exception.DownstreamBadResponseException;
import com.talentgrid.workforce.aiupskill.exception.DownstreamTimeoutException;
import com.talentgrid.workforce.aiupskill.exception.DownstreamUnavailableException;
import com.talentgrid.workforce.aiupskill.exception.EmployeeNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class TalentGridFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String url = response.request().url();

        if (status == 404) {
            // Check if this is the engineer lookup endpoint
            if (url != null && (url.contains("/api/v1/engineers/") || url.contains("/api/v1/engineer-profile/employees/"))) {
                return new EmployeeNotFoundException("Employee profile not found in downstream service.");
            }
            return new DownstreamBadResponseException("Downstream resource not found: " + url);
        }

        if (status == 408 || status == 504) {
            return new DownstreamTimeoutException("Downstream service timed out.");
        }

        if (status == 500 || status == 502 || status == 503) {
            return new DownstreamUnavailableException("Downstream service is currently unavailable.");
        }

        // Default fallback
        return defaultDecoder.decode(methodKey, response);
    }
}
