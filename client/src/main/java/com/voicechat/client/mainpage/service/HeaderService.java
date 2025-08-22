package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;

public class HeaderService {

    public ServerResponse searchUser(String field) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        User user = new User();
        user.setDisplayName(field);
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_SEARCH, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));



        String serverInLine = Listener.getServerIn().readLine();
        return mapper.readValue(serverInLine, ServerResponse.class);
    }

}
