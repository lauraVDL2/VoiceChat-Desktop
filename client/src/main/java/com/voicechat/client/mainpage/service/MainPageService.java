package com.voicechat.client.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import org.shared.*;
import org.shared.entity.Conversation;
import org.shared.entity.User;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MainPageService {

    public ServerResponse createConversation(Conversation conversation) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_CREATE, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse scrollMessages(Conversation conversation, int offset) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.CONVERSATION_SCROLL, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        message.setOffset(offset);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public void displayUserConversations(User user, String correlationId) throws IOException, InterruptedException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(user);
        Message message = new Message(MessageType.CONVERSATION_DISPLAY, json);
        message.setCorrelationId(correlationId);
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

    public void sendAvatarInfo(String correlationId, User targetUser) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(targetUser);
        Message message = new Message(MessageType.READ_TARGET_AVATAR, json);
        message.setCorrelationId(correlationId);
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
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        System.out.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse sendMessage(String correlationId, Conversation conversation) throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.MESSAGE_SEND, json);
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse searchMessageInConversation(Conversation conversation) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.MESSAGE_CONVERSATION_SEARCH, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    public ServerResponse goToMessageInConversation(Conversation conversation) throws Exception {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        String json = mapper.writeValueAsString(conversation);
        Message message = new Message(MessageType.MESSAGE_CONVERSATION_GO, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(mapper.writeValueAsString(message));

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

}
