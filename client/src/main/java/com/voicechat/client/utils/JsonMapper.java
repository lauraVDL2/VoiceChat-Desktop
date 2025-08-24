package com.voicechat.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapper {

    private static ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static ObjectMapper getJsonMapper() {
        return objectMapper;
    }
}
