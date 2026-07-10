package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.session.SessionFactory;
import org.server.Server;
import org.server.config.Neo4jConfig;
import org.server.dao.ConversationDao;
import org.server.dao.ConversationDaoImpl;
import org.server.dao.UserDao;
import org.server.dao.UserDaoImpl;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserNotificationAction {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationAction.class);

    private final SessionFactory sessionFactory = Neo4jConfig.getSessionFactory();

    private final UserDao userDao;

    private final ConversationDao conversationDao;

    public UserNotificationAction(UserDao userDao, ConversationDao conversationDao) {
        this.userDao = userDao;
        this.conversationDao = conversationDao;
    }

    public void sendMessageToUser(org.shared.Message messageObj, ConcurrentHashMap<String, Socket> userSockets,
                                  ObjectMapper objectMapper, ServerResponse serverResponse,
                                  Conversation conversation) throws IOException {
        List<String> emailAddresses = conversationDao.getConversationParticipants(conversation)
                .stream().map(User::getEmailAddress).toList();
        List<Message> messages = conversation.getMessages();
        Message lastMessage = messages.get(messages.size() - 1);
        User sender = userDao.findUserByEmailAddress(lastMessage.getSender().getEmailAddress());
        for (var userSocket : userSockets.entrySet()) {
            if (!CollectionUtils.isEmpty(emailAddresses)) {
                String emailAddress = userSocket.getKey();
                Socket socket = userSocket.getValue();
                if (emailAddresses.contains(emailAddress)
                        && !StringUtils.equals(sender.getEmailAddress(), emailAddress)) {
                    serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.NEW_MESSAGE_NOTIFIED);
                    serverResponse.getUserMessageMap().computeIfAbsent("notif-" + emailAddress, k -> "notif-" + messageObj.getCorrelationId());
                    serverResponse.setCorrelationId("notif-" + messageObj.getCorrelationId());
                    serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(conversation));
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
                    dataOutputStream.writeUTF("JSON_RESPONSE");
                    dataOutputStream.writeInt(bytes.length);
                    dataOutputStream.write(bytes);
                    dataOutputStream.flush();

                    //searchSentAvatar(objectMapper, sender, socket, serverResponse);
                    searchSentAvatar(emailAddress, messageObj, objectMapper, sender, socket, serverResponse);
                }
            }
        }
    }

    /**
     * Avatar display of the user when a message is sent
     * @param emailAddress
     * @param messageObj
     * @param objectMapper
     * @param sender
     * @param socket
     * @param serverResponse
     * @throws IOException
     */
    private void searchSentAvatar(String emailAddress, org.shared.Message messageObj, ObjectMapper objectMapper, User sender,
                                 Socket socket, ServerResponse serverResponse) throws IOException {
        if (sender != null) {
            if (StringUtils.isNotBlank(sender.getAvatar())) {
                byte[] avatarBytes = getAvatarBytes(sender.getAvatar());
                serverResponse.setBinaryPayload(avatarBytes);
                serverResponse.getUserMessageMap().computeIfAbsent(emailAddress, k -> "notif-" + messageObj.getCorrelationId());
                serverResponse.setCorrelationId("notif-" + messageObj.getCorrelationId());
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.READ_TARGET_AVATAR);

                byte[] jsonBytes = objectMapper.writeValueAsBytes(serverResponse);

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF("IMAGE_RESPONSE");
                dataOutputStream.writeInt(jsonBytes.length);
                dataOutputStream.write(jsonBytes);
                dataOutputStream.flush();
                System.out.println("OK [" + new String(jsonBytes, StandardCharsets.UTF_8) + "]" + jsonBytes.length);
            }
        }
    }

    public byte[] getAvatarBytes(String resourcePath) throws IOException {
        try (InputStream is = Server.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.error("Resource not found: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }
}
