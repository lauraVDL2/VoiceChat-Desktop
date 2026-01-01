package com.voicechat.client.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.common.UserSession;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;

import java.io.PrintWriter;
import java.util.UUID;

public class ConnectService {

    public ServerResponse getMicrosoftAuthentication() throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(UserSession.INSTANCE.getUser());
        Message message = new Message(MessageType.MICROSOFT_AUTHENTICATE, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(objectMapper.writeValueAsString(message));
            serverOut.flush();
        }

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }
}
