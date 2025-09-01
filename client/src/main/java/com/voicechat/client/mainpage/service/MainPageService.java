package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.*;
import org.shared.entity.Conversation;
import org.shared.entity.User;

import java.io.*;

public class MainPageService {

    public ServerResponse createConversation(Conversation conversation) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_CREATE, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponse();

        /*String serverInLine = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInLine, ServerResponse.class);*/
    }

    public void displayUserConversations(User user) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(user);
        Message message = new Message(MessageType.CONVERSATION_DISPLAY, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        /*InputStream inputStream = Listener.getSocket().getInputStream();
        BufferedReader reader = Listener.getServerIn();
        DataInputStream dataInputStream = new DataInputStream(inputStream);

// Step 1: Read the header line
        String header = dataInputStream.readUTF();

        System.out.println("header = " + header);

        if ("_RESPONSE".equals(header)) {
            // Step 2: Read the size of the payload
            int length = dataInputStream.readInt();
            System.out.println("len = " + length);

            // Step 3: Read the payload bytes
            byte[] payloadBytes = new byte[length];
            dataInputStream.readFully(payloadBytes);
            System.out.println("toto");
            // Now you can deserialize the payloadBytes as needed
            ServerResponse serverResponse = objectMapper.readValue(payloadBytes, ServerResponse.class);
            return serverResponse;
            // Process serverResponse...
        } else {
            // Handle other types of responses or errors
            return new ServerResponse();
        }*/

        /*DataInputStream dis = new DataInputStream(Listener.getSocket().getInputStream());
        int length = dis.readInt();
        byte[] responseBytes = new byte[length];
        System.out.println("len = " + length);
        dis.readFully(responseBytes);
        System.out.println("GO HERE");
        return objectMapper.readValue(responseBytes, ServerResponse.class);*/

        /*byte[] bytes = Listener.getSocket().getInputStream().readAllBytes();

        System.out.println("VA BIEN ICI");

        return objectMapper.readValue(bytes, ServerResponse.class);*/
    }

    public void sendAvatarInfo(User targetUser) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(targetUser);
        Message message = new Message(MessageType.READ_TARGET_AVATAR, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        //System.out.println(Listener.getServerIn().readLine());

        /*String serverInline = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInline, ServerResponse.class);*/
    }

    public ServerResponse getConversation(Conversation conversation) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_GET, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponse();

        /*String serverInLine = Listener.getServerIn().readLine();
        System.out.println("CONVERSATION GET = " + serverInLine);
        return objectMapper.readValue(serverInLine, ServerResponse.class);*/
    }

    public ServerResponse sendMessage(Conversation conversation) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.MESSAGE_SEND, json);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponse();
        /*String serverInLine = Listener.getServerIn().readLine();
        return objectMapper.readValue(serverInLine, ServerResponse.class);*/
    }
}
