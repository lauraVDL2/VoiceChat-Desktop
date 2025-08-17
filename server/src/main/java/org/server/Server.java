package org.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.server.dao.UserDao;
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
                            User user = objectMapper.readValue(messageObj.getMsg(), User.class);
                            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                            user.setPassword(hashedPassword);
                            UserDao userDao = new UserDao();
                            if (userDao.saveUser(user)) {
                                System.out.println("User saved !");
                                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                                serverResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
                                out.println(objectMapper.writeValueAsString(serverResponse));
                            }
                            userDao.close();
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

}
