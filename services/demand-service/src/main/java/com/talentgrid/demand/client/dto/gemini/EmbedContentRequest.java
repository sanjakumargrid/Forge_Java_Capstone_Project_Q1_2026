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

    public static EmbedContentRequest of(String model, String text) {
        return new EmbedContentRequest(model, new Content(Collections.singletonList(new Part(text))));
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
