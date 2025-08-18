package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.server.dao.UserDao;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    public static void userCreate(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        UserDao userDao = new UserDao();
        if (userDao.saveUser(user)) {
            logger.info("User saved !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        else {
            logger.error("Registration failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            serverResponse.setMessage(UserDao.errorMessage);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
    }

    public static void userLogIn(ObjectMapper objectMapper, Message messageObj,
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
        }
        else {
            logger.error("Connection failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setMessage(UserDao.errorMessage);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
    }
}
