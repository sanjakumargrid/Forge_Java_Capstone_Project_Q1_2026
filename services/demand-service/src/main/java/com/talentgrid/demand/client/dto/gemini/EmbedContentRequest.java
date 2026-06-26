package com.talentgrid.demand.client.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedContentRequest {
    private String model;
    private Content content;
    private Integer outputDimensionality;

    public static EmbedContentRequest of(String model, String text, int outputDimensionality) {
        return new EmbedContentRequest(
                model,
                new Content(Collections.singletonList(new Part(text))),
                outputDimensionality);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}
