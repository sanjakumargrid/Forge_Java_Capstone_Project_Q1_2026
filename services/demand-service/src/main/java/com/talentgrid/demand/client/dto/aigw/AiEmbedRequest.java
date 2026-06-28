package com.talentgrid.demand.client.dto.aigw;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiEmbedRequest {

    private String text;
    private String model;
}
