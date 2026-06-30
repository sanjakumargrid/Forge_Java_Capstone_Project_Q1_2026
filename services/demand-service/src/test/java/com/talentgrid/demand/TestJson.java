package com.talentgrid.demand;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.talentgrid.demand.dto.request.DemandRequest;

public class TestJson {
    public static void main(String[] args) {
        String json = "{\n" +
                "  \"title\": \"Senior Java and Springboot Developer\",\n" +
                "  \"description\": \"Looking for a backend developer with strong Spring Boot and microservices experience.\",\n" +
                "  \"level\": \"T3\",\n" +
                "  \"location\": \"Bangalore\",\n" +
                "  \"accountId\": 2053,\n" +
                "  \"projectId\": 9063,\n" +
                "  \"businessUnit\": \"Cloud Engineering\",\n" +
                "  \"skills\": [\n" +
                "    \"Java\",\n" +
                "    \"Spring Boot\",\n" +
                "    \"Microservices\",\n" +
                "    \"PostgreSQL\"\n" +
                "  ],\n" +
                "  \"budget\": 2500000.00,\n" +
                "  \"reqUtilPerc\": 100,\n" +
                "  \"requiredCount\": 1,\n" +
                "  \"targetDate\": \"2026-08-01\",\n" +
                "  \"priority\": \"HIGH\",\n" +
                "  \"employmentType\": \"FULL_TIME\",\n" +
                "  \"searchStartAt\": \"2026-06-22T09:00:00Z\"\n" +
                "}";

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            DemandRequest req = mapper.readValue(json, DemandRequest.class);
            System.out.println("Parsed successfully: " + req.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
