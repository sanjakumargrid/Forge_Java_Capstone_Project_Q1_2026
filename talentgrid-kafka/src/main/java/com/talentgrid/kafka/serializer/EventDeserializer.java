package com.talentgrid.kafka.serializer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.kafka.events.base.BaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventDeserializer {

    private final ObjectMapper mapper;
    public <T> BaseEvent<T> deserialize(String json, Class<T> payloadType)
            throws Exception {
        JavaType type = mapper.getTypeFactory()
                .constructParametricType(BaseEvent.class, payloadType);
        return mapper.readValue(json, type);
    }
}