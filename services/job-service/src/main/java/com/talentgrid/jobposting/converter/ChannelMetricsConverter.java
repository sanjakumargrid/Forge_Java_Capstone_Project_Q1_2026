package com.talentgrid.jobposting.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.jobposting.dto.embedded.ChannelMetricsDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class ChannelMetricsConverter implements AttributeConverter<List<ChannelMetricsDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ChannelMetricsDto> attribute) {
        if (attribute == null) return null;
        try { return MAPPER.writeValueAsString(attribute); } catch (Exception e) { return null; }
    }

    @Override
    public List<ChannelMetricsDto> convertToEntityAttribute(String dbData) {
        if (dbData == null) return List.of();
        try { return MAPPER.readValue(dbData, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }
}
