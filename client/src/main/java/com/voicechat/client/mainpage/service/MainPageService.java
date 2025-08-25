package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.Conversation;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MainPageService {

    public ServerResponse createConversation(Conversation conversation) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_CREATE, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInLine, ServerResponse.class);
    }

    public ServerResponse displayUserConversations(User user) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(user);
        Message message = new Message(MessageType.CONVERSATION_DISPLAY, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInline = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInline, ServerResponse.class);
    }

    public void sendAvatarInfo(User targetUser) throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(targetUser);
        Message message = new Message(MessageType.READ_TARGET_AVATAR, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));
    }

    public ServerResponse getConversation(Conversation conversation) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_GET, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInLine, ServerResponse.class);
    }
}
