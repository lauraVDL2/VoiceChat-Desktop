package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

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

        serverOut.println(mapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse searchConversationIfExists(List<User> users) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(users);
        Message message = new Message(MessageType.CONVERSATION_SEARCH, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

}
