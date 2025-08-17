package com.voicechat.client.login.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterService {

    private final static String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public boolean verifyEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public ServerResponse register(User user) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);
        Message message = new Message(MessageType.USER_CREATE, json);
        PrintWriter serverOut = Listener.getServerOut();

        // Send the request
        serverOut.println(mapper.writeValueAsString(message));

        // Read response directly
        String serverInLine = Listener.getServerIn().readLine();
        return mapper.readValue(serverInLine, ServerResponse.class);
    }

}
