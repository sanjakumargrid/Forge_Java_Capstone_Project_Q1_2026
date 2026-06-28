package com.talentgrid.demand.client.dto.aigw;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiLlmCompleteRequest {

    private String featureType;
    private String prompt;
    private String systemPrompt;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
