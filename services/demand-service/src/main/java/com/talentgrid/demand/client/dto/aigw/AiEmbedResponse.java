package com.talentgrid.demand.client.dto.aigw;

import lombok.Data;

import java.util.List;

@Data
public class AiEmbedResponse {

    private List<Float> embedding;
    private String model;
    private Long latencyMs;
}
