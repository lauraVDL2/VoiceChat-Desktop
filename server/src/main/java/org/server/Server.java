package org.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.server.action.ConversationAction;
import org.server.action.MessageAction;
import org.server.action.UserAction;
import org.server.action.UserNotificationAction;
import org.server.dao.MessageDao;
import org.server.dao.UserDao;
import org.shared.entity.Conversation;
import org.shared.entity.User;
import org.shared.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final static int SERVER_PORT = 8080;
    private final static ExecutorService executor = Executors.newCachedThreadPool();
    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private final static ConcurrentHashMap<String, UserSessionStatus> onlineUsers = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Socket> userSockets = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            logger.info("Server started, waiting for clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClientAsync(clientSocket);
            }
        }
    }

    public static void handleClientAsync(Socket socket) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                String message;
                while ((message = in.readLine()) != null) {
                    logger.info(message);
                    ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    ServerResponse serverResponse = new ServerResponse();
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    UserAction userAction = null;
                    ConversationAction conversationAction = null;
                    MessageAction messageAction = null;
                    UserNotificationAction userNotificationAction = null;
                    switch (messageObj.getMessageType()) {
                        case USER_CREATE:
                            userAction = new UserAction();
                            User userCreated = userAction.userCreate(objectMapper, messageObj, serverResponse, socket);
                            if (userCreated != null) {
                                userAction.searchUserAvatar(objectMapper, userCreated, dataOutputStream, serverResponse);
                            }
                            break;
                        case USER_LOG_IN:
                            userAction = new UserAction();
                            User userLogged = userAction.userLogIn(objectMapper, messageObj, serverResponse, socket);
                            if (userLogged != null) {
                                userAction.searchUserAvatar(objectMapper, userLogged, dataOutputStream, serverResponse);
                            }
                            onlineUsers.computeIfAbsent(userLogged.getEmailAddress(), status -> UserSessionStatus.ONLINE);
                            userSockets.computeIfAbsent(userLogged.getEmailAddress(), mySocket -> socket);
                            break;
                        case USER_EXIT:
                            User userExit = objectMapper.readValue(messageObj.getPayload(), User.class);
                            String emailAddress = userExit.getEmailAddress();
                            onlineUsers.remove(emailAddress);
                            userSockets.get(emailAddress).close();
                            userSockets.remove(emailAddress);
                            break;
                        case USER_SEARCH:
                            userAction = new UserAction();
                            userAction.userSearch(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case ONLINE_USERS_FETCH:
                            userAction = new UserAction();
                            userAction.getOnlineUsers(objectMapper, serverResponse, socket, onlineUsers);
                            break;
                        case CONVERSATION_SEARCH:
                            conversationAction = new ConversationAction();
                            conversationAction.conversationSearchIfExists(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case CONVERSATION_CREATE:
                            conversationAction = new ConversationAction();
                            conversationAction.createConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case CONVERSATION_DISPLAY:
                            conversationAction = new ConversationAction();
                            conversationAction.searchUserConversations(objectMapper, messageObj, serverResponse, out, socket);
                            break;
                        case READ_TARGET_AVATAR:
                            userAction = new UserAction();
                            userAction.searchTargetUser(objectMapper, messageObj, dataOutputStream, serverResponse);
                            break;
                        case CONVERSATION_GET:
                            conversationAction = new ConversationAction();
                            conversationAction.getConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case MESSAGE_SEND:
                            sendMessageToUser(objectMapper, messageObj, serverResponse, socket);
                            break;
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void sendMessageToUser(ObjectMapper objectMapper, Message messageObj,
                                         ServerResponse serverResponse, Socket socket) {
        try {
            MessageAction messageAction = new MessageAction();
            Conversation messageSentConversation = messageAction.sendMessage(objectMapper, messageObj, serverResponse, socket);
            UserNotificationAction userNotificationAction = new UserNotificationAction();
            userNotificationAction.sendMessageToUser(userSockets, objectMapper, serverResponse,
                    messageSentConversation);
        } catch (Exception e) {
            logger.error("Error sending message: ", e);
        }
    }

}
