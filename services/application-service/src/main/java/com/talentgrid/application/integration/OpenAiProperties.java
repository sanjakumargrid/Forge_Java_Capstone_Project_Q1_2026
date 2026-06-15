package com.talentgrid.application.integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

//    private String apiKey;
//    private String model = "gpt-4o-mini";
//    private String baseUrl = "https://api.openai.com/v1";
}