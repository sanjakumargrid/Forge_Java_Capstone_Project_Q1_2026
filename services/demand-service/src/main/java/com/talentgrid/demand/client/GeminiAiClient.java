package com.talentgrid.demand.client;

import com.talentgrid.demand.client.dto.gemini.EmbedContentRequest;
import com.talentgrid.demand.client.dto.gemini.EmbedContentResponse;
import com.talentgrid.demand.client.dto.gemini.GenerateContentRequest;
import com.talentgrid.demand.client.dto.gemini.GenerateContentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "geminiAiClient", url = "${gemini.api.base-url}")
public interface GeminiAiClient {

    @PostMapping("/v1beta/models/{model}:embedContent")
    EmbedContentResponse embedContent(
            @PathVariable("model") String model,
            @RequestParam("key") String apiKey,
            @RequestBody EmbedContentRequest request
    );

    @PostMapping("/v1beta/models/{model}:generateContent")
    GenerateContentResponse generateContent(
            @PathVariable("model") String model,
            @RequestParam("key") String apiKey,
            @RequestBody GenerateContentRequest request
    );
}
