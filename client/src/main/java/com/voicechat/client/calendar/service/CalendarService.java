package com.voicechat.client.calendar.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.mapped_entity.VoiceChatCalendar;
import org.shared.mapped_entity.VoiceChatEvent;

import java.io.PrintWriter;
import java.util.UUID;

public class CalendarService {

    public ServerResponse getEvents(VoiceChatCalendar voiceChatCalendar) throws JsonProcessingException, InterruptedException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(voiceChatCalendar);
        Message message = new Message(MessageType.EVENTS_GET, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse createEvent(VoiceChatEvent voiceChatEvent) throws InterruptedException, JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(voiceChatEvent);
        Message message = new Message(MessageType.EVENT_CREATE, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }
}
