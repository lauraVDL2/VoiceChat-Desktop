package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.utils.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class HeaderService {

    public ServerResponse searchUser(String field) throws IOException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        User user = new User();
        user.setDisplayName(field);
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_SEARCH, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));



        String serverInLine = Listener.getServerIn().readLine();
        return mapper.readValue(serverInLine, ServerResponse.class);
    }

    public ServerResponse searchConversationIfExists(List<User> users) throws IOException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(users);
        Message message = new Message(MessageType.CONVERSATION_SEARCH, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();
        return mapper.readValue(serverInLine, ServerResponse.class);
    }

}
