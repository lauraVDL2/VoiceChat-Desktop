package org.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.server.dao.UserDao;
import org.shared.Message;
import org.shared.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
                    out.println("Echo: " + message);
                    ObjectMapper objectMapper = new ObjectMapper();
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    switch (messageObj.getMessageType()) {
                        case USER:
                            User user = objectMapper.readValue(messageObj.getMsg(), User.class);
                            UserDao userDao = new UserDao();
                            userDao.saveUser(user);
                            System.out.println("User saved !");
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
