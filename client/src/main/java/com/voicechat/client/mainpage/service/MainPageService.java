package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voicechat.client.Listener;
import com.voicechat.client.utils.JsonMapper;
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
        objectMapper.registerModule(new JavaTimeModule());
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_CREATE, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInLine, ServerResponse.class);
    }
}
