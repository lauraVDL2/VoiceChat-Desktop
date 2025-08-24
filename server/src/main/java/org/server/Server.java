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
import org.server.action.UserAction;
import org.server.dao.UserDao;
import org.shared.entity.User;
import org.shared.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final static int SERVER_PORT = 8080;
    private final static ExecutorService executor = Executors.newCachedThreadPool();
    private final static Logger logger = LoggerFactory.getLogger(Server.class);

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
                    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    ServerResponse serverResponse = new ServerResponse();
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    UserAction userAction = null;
                    ConversationAction conversationAction = null;
                    switch (messageObj.getMessageType()) {
                        case USER_CREATE:
                            userAction = new UserAction();
                            User userCreated = userAction.userCreate(objectMapper, messageObj, serverResponse, out);
                            if (userCreated != null) {
                                readMyAvatar(dataOutputStream, userAction, userCreated);
                            }
                            break;
                        case USER_LOG_IN:
                            userAction = new UserAction();
                            User userLogged = userAction.userLogIn(objectMapper, messageObj, serverResponse, out);
                            if (userLogged != null) {
                                readMyAvatar(dataOutputStream, userAction, userLogged);
                            }
                            break;
                        case USER_SEARCH:
                            userAction = new UserAction();
                            userAction.userSearch(objectMapper, messageObj, serverResponse, out);
                            break;
                        case CONVERSATION_SEARCH:
                            conversationAction = new ConversationAction();
                            conversationAction.conversationSearchIfExists(objectMapper, messageObj, serverResponse, out);
                            break;
                        case CONVERSATION_CREATE:
                            conversationAction = new ConversationAction();
                            conversationAction.createConversation(objectMapper, messageObj, serverResponse, out);
                            break;
                        case CONVERSATION_DISPLAY:
                            conversationAction = new ConversationAction();
                            conversationAction.searchUserConversations(objectMapper, messageObj, serverResponse, out);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void readMyAvatar(DataOutputStream dataOutputStream, UserAction userAction, User user) throws IOException {
        String avatarPath = user.getAvatar();
        if (StringUtils.isNotBlank(avatarPath)) {
            byte[] avatarBytes = userAction.getAvatarBytes(user.getAvatar());
            dataOutputStream.writeInt(avatarBytes.length);
            dataOutputStream.write(avatarBytes);
            dataOutputStream.flush();
        }
    }

}
