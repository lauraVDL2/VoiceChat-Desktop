package com.voicechat.client.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;

public class LoginService {

    public ServerResponse login(User user) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_LOG_IN, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();
        return  mapper.readValue(serverInLine, ServerResponse.class);
    }
}
