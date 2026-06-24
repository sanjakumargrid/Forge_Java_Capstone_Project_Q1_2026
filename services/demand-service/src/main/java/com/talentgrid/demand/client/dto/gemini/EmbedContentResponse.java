package com.talentgrid.demand.client.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbedContentResponse {
    private Embedding embedding;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedding {
        private float[] values;
    }
}
