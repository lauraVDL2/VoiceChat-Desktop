package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.server.Server;
import org.server.dao.UserDao;
import org.shared.*;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    public User userCreate(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, Socket socket) throws IOException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setAvatar("/images/avatar/avatar_default.png");
        UserDao userDao = new UserDao();
        byte[] bytes = null;
        if (userDao.saveUser(user)) {
            logger.info("User saved !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            return user;
        }
        else {
            logger.error("Registration failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            serverResponse.setMessage(UserDao.errorMessage);
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
        return null;
    }

    public User userLogIn(ObjectMapper objectMapper, Message messageObj,
                                 ServerResponse serverResponse, Socket socket) throws IOException {
        User userLogged = objectMapper.readValue(messageObj.getPayload(), User.class);
        UserDao userDao = new UserDao();
        User resultUser = userDao.login(userLogged);
        byte[] bytes = null;
        if (resultUser != null) {
            logger.info("User connected !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(resultUser));
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            System.out.println("AFTER JSON RESPONSE" + new String(bytes, StandardCharsets.UTF_8));
            return resultUser;
        }
        else {
            logger.error("Connection failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setMessage(UserDao.errorMessage);
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
        return null;
    }

    public void userSearch(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, Socket socket) throws IOException {
        User userSearch = objectMapper.readValue(messageObj.getPayload(), User.class);
        String displayNameSearch = userSearch.getDisplayName();
        UserDao userDao = new UserDao();
        List<User> users = userDao.searchUsers(displayNameSearch);
        byte[] bytes = null;
        if (!CollectionUtils.isEmpty(users)) {
            logger.info("Users found !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_SEARCHED);
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(users));
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
        else {
            logger.info("Users not found !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_SEARCHED);
            serverResponse.setMessage("No user found !");
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    public void getOnlineUsers(ObjectMapper objectMapper,
                               ServerResponse serverResponse, Socket socket,
                               ConcurrentHashMap<String, UserSessionStatus> onlineUsers) throws IOException {
        serverResponse.setServerResponseMessage(ServerResponseMessage.ONLINE_USERS_FETCHED);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        ServerInformation serverInformation = new ServerInformation();
        serverInformation.setOnlineUsers(onlineUsers);
        serverResponse.setServerInformation(serverInformation);
        byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeUTF("JSON_RESPONSE");
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }

    public void searchTargetUser(ObjectMapper objectMapper, Message messageObj,
                                 DataOutputStream dataOutputStream, ServerResponse serverResponse) throws IOException {
        User targetUser = objectMapper.readValue(messageObj.getPayload(), User.class);
        if (targetUser != null) {
            if (StringUtils.isNotBlank(targetUser.getAvatar())) {
                byte[] avatarBytes = getAvatarBytes(targetUser.getAvatar());
                serverResponse.setBinaryPayload(avatarBytes);
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.READ_TARGET_AVATAR);

                byte[] jsonBytes = objectMapper.writeValueAsBytes(serverResponse);
                dataOutputStream.writeUTF("IMAGE_RESPONSE");
                dataOutputStream.writeInt(jsonBytes.length);
                dataOutputStream.write(jsonBytes);
                dataOutputStream.flush();
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
