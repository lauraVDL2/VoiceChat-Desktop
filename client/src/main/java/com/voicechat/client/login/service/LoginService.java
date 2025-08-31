package com.voicechat.client.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;

public class LoginService {

    public void login(User user) throws IOException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_LOG_IN, json);
        PrintWriter serverOut = Listener.getServerOut();

        System.out.println(mapper.writeValueAsString(message));
        serverOut.println(mapper.writeValueAsString(message));

        /*String serverInLine = Listener.getServerIn().readLine();
        System.out.println(serverInLine);
        return  mapper.readValue(serverInLine, ServerResponse.class);*/
    }
}
