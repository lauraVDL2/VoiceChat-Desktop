package org.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.server.dao.UserDao;
import org.shared.entity.User;
import org.shared.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final static int SERVER_PORT = 8080;
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started, waiting for clients...");
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
                    System.out.println("Received: " + message);
                    ObjectMapper objectMapper = new ObjectMapper();
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    ServerResponse serverResponse = new ServerResponse();
                    switch (messageObj.getMessageType()) {
                        case USER_CREATE:
                            userCreate(objectMapper, messageObj, serverResponse, out);
                            break;
                        case USER_LOG_IN:
                            userLogIn(objectMapper, messageObj, serverResponse, out);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void userCreate(ObjectMapper objectMapper, Message messageObj,
                                  ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        UserDao userDao = new UserDao();
        if (userDao.saveUser(user)) {
            System.out.println("User saved !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        else {
            System.out.println("Registration failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
    }

    public static void userLogIn(ObjectMapper objectMapper, Message messageObj,
                                 ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User userLogged = objectMapper.readValue(messageObj.getPayload(), User.class);
        UserDao userDao = new UserDao();
        User resultUser = userDao.login(userLogged);
        if (resultUser != null) {
            System.out.println("User connected !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            serverResponse.setPayload(objectMapper.writeValueAsString(resultUser));
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
        else {
            System.out.println("Connection failed !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.USER_LOGGED_IN);
            out.println(objectMapper.writeValueAsString(serverResponse));
        }
    }

}
