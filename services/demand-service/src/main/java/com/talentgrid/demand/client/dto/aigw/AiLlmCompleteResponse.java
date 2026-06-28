package com.talentgrid.demand.client.dto.aigw;

import lombok.Data;

@Data
public class AiLlmCompleteResponse {

    private boolean success;
    private String text;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long latencyMs;
    private String error;
}
