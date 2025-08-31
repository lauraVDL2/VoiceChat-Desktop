package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.server.dao.ConversationDao;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ConversationAction {

    private static final Logger logger = LoggerFactory.getLogger(ConversationAction.class);

    public void getConversation(ObjectMapper objectMapper, Message messageObj,
                                ServerResponse serverResponse, Socket socket) throws IOException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        if (conversation != null) {
            ConversationDao conversationDao = new ConversationDao();
            Conversation fullConversation = conversationDao.getConversation(conversation);
            byte[] bytes = null;
            if (fullConversation != null) {
                logger.info("Conversation found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_GET);
                serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(fullConversation));
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                logger.error("Conversation not found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_GET);
                serverResponse.setMessage("Conversation not found !");
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
    }

    public void searchUserConversations(ObjectMapper objectMapper, Message messageObj,
                                        ServerResponse serverResponse, PrintWriter out, Socket socket) throws IOException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        if (user != null) {
            ConversationDao conversationDao = new ConversationDao();
            List<Conversation> conversations = conversationDao.searchUserConversations(user);
            byte[] bytes =  null;
            if (!CollectionUtils.isEmpty(conversations)) {
                logger.info("Conversations found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_DISPLAYED);
                serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(conversations));
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                // Sending side
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                logger.info("No conversation found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.INFO);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_DISPLAYED);
                serverResponse.setMessage("No conversations yet, start a new one !");
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeBytes("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
    }

    public void createConversation(ObjectMapper objectMapper, Message messageObj,
                                   ServerResponse serverResponse, Socket socket) throws IOException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        if (conversation != null) {
            Set<User> participants = conversation.getParticipants();
            org.shared.entity.Message message = conversation.getMessages().get(0);
            ConversationDao conversationDao = new ConversationDao();
            Conversation newConversation = conversationDao.createConversation(participants, message, message.getSender());
            byte[] bytes = null;
            if (newConversation != null) {
                logger.info("Conversation created !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_CREATED);
                serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(newConversation));
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                logger.info("Conversation could not be created !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_CREATED);
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
    }

    public void conversationSearchIfExists(ObjectMapper objectMapper, Message messageObj,
                           ServerResponse serverResponse, Socket socket) throws IOException {
        List<User> users = objectMapper.readValue(
                messageObj.getPayload(),
                new TypeReference<List<User>>() {}
        );
        if (!CollectionUtils.isEmpty(users)) {
            if (users.size() > 1) {
                User currentUser = users.get(0);
                User targetUser = users.get(1);
                ConversationDao conversationDao = new ConversationDao();
                Conversation conversation = conversationDao.searchConversationIfExists(currentUser, targetUser);
                byte[] bytes = null;
                if (conversation != null) {
                    logger.info("Conversation found !");
                    serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);
                    serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(conversation));
                    bytes = objectMapper.writeValueAsBytes(serverResponse);
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF("JSON_RESPONSE");
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                    outputStream.flush();
                }
                else {
                    logger.info("No conversation found !");
                    serverResponse.setServerResponseStatus(ServerResponseStatus.INFO);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);
                    bytes = objectMapper.writeValueAsBytes(serverResponse);
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF("JSON_RESPONSE");
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                    outputStream.flush();
                }
            }
        }
    }
}
