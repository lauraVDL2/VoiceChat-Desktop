package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HeaderService {

    public ServerResponse searchUser(String field) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        User user = new User();
        user.setDisplayName(field);
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_SEARCH, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(mapper.writeValueAsString(message));
            serverOut.flush();
        }

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public void sendAvatarInfo(String correlationId, User targetUser) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(targetUser);
        Message message = new Message(MessageType.READ_TARGET_AVATAR, json);
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(objectMapper.writeValueAsString(message));
            serverOut.flush();
        }
    }

    public ServerResponse searchConversationIfExists(List<User> users) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(users);
        Message message = new Message(MessageType.CONVERSATION_SEARCH, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(mapper.writeValueAsString(message));
            serverOut.flush();
        }

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

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
