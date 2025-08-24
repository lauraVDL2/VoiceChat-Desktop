package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.server.dao.UserDao;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    public User userCreate(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setAvatar("/images/avatar/avatar_default.png");
        UserDao userDao = new UserDao();
        if (userDao.saveUser(user)) {
            logger.info("User saved !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            out.println(objectMapper.writeValueAsString(serverResponse));
            return user;
        }
        else {
            logger.error("Registration failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            serverResponse.setMessage(UserDao.errorMessage);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        return null;
    }

    public User userLogIn(ObjectMapper objectMapper, Message messageObj,
                                 ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User userLogged = objectMapper.readValue(messageObj.getPayload(), User.class);
        UserDao userDao = new UserDao();
        User resultUser = userDao.login(userLogged);
        if (resultUser != null) {
            logger.info("User connected !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setPayload(objectMapper.writeValueAsString(resultUser));
            out.println(objectMapper.writeValueAsString(serverResponse));
            return resultUser;
        }
        else {
            logger.error("Connection failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setMessage(UserDao.errorMessage);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        return null;
    }

    public void userSearch(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User userSearch = objectMapper.readValue(messageObj.getPayload(), User.class);
        String displayNameSearch = userSearch.getDisplayName();
        UserDao userDao = new UserDao();
        List<User> users = userDao.searchUsers(displayNameSearch);
        if (!CollectionUtils.isEmpty(users)) {
            logger.info("Users found !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_SEARCHED);
            serverResponse.setPayload(objectMapper.writeValueAsString(users));
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        else {
            logger.info("Users not found !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_SEARCHED);
            serverResponse.setMessage("No user found !");
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
    }

    public void searchTargetUser(ObjectMapper objectMapper, Message messageObj,
                                 DataOutputStream dataOutputStream) throws IOException {
        User targetUser = objectMapper.readValue(messageObj.getPayload(), User.class);
        if (targetUser != null) {
            if (StringUtils.isNotBlank(targetUser.getAvatar())) {
                byte[] avatarBytes = getAvatarBytes(targetUser.getAvatar());
                dataOutputStream.writeInt(avatarBytes.length);
                dataOutputStream.write(avatarBytes);
                dataOutputStream.flush();
            }
        }
    }

    public byte[] getAvatarBytes(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.error("Resource not found: " + resourcePath);
            }
            return is.readAllBytes(); // Java 9+; for earlier versions, use a buffer loop
        }
    }
}
