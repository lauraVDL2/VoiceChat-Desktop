package org.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;
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
                    ObjectMapper objectMapper = new ObjectMapper();
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    ServerResponse serverResponse = new ServerResponse();
                    switch (messageObj.getMessageType()) {
                        case USER_CREATE:
                            UserAction.userCreate(objectMapper, messageObj, serverResponse, out);
                            break;
                        case USER_LOG_IN:
                            UserAction.userLogIn(objectMapper, messageObj, serverResponse, out);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }



}
