package com.voicechat.client.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.PrintWriter;
import java.util.UUID;

public class SettingsService {

    public ServerResponse setThemeMode(User user) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.THEME_MODE_SET, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(mapper.writeValueAsString(message));
            serverOut.flush();
        }

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }
}
