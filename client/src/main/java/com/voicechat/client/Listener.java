package com.voicechat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.login.controller.ConnectController;
import com.voicechat.client.login.controller.LoginController;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.shared.ServerResponse;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class Listener {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final RetryConfig config = RetryConfig.custom()
            .maxAttempts(4)
            .waitDuration(Duration.ofMillis(5000))
            .build();

    private static final Retry retry = Retry.of("socketRetry", config);

    private static Socket socket;

    private static PrintWriter serverOut;

    private static BufferedReader serverIn;

    private static DataInputStream dataInputStream;

    public Listener() {

    }

    public static void connect(LoginController loginController, ConnectController connectController, Stage stage, Parent loginRoot) {
        displayConnectPanel(connectController);
        // Wrap connection logic in a supplier
        Supplier<Socket> socketSupplier = () -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                return socket;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // Execute with retry
        CompletableFuture<Socket> socketFuture = CompletableFuture.supplyAsync(
                Retry.decorateSupplier(retry, socketSupplier),
                executor
        );

        socketFuture.thenAcceptAsync(socket -> {
            try {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serverOut = new PrintWriter(socket.getOutputStream(), true);
                dataInputStream = new DataInputStream(socket.getInputStream());

                displayLogPanel(loginController, stage, loginRoot);

                // Asynchronously read messages from server
                /*CompletableFuture.runAsync(() -> {
                    String messageFromServer;
                    try {
                        while ((messageFromServer = serverIn.readLine()) != null) {
                            System.out.println("Server: " + messageFromServer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, executor);*/

                // Send user input to server
                String userMessage;
                /*while ((userMessage = userInput.readLine()) != null) {
                    serverOut.println(userMessage);
                    *//*ObjectMapper objectMapper = new ObjectMapper();
                    ServerResponse response = objectMapper.readValue(userMessage, ServerResponse.class);
                    switch (response.getServerResponseStatus()) {
                        case SUCCESS:
                            switch (response.getServerResponseMessage()) {
                                case USER_CREATED:
                            }
                            break;
                    }*//*
                }*/
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void displayLogPanel(LoginController loginController, Stage stage, Parent loginRoot) {
        // Notify UI that connection is successful
        Platform.runLater(() -> {
            if (loginController != null) {
                Scene scene = new Scene(loginRoot,  300, 300);
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/login.css").toExternalForm());
                stage.setScene(scene);
                loginController.onConnected();
            }
        });
    }

    public static void displayConnectPanel(ConnectController connectController) {
        /*Platform.runLater(() -> {
            if (connectController != null) {
                connectController.showConnecting();
            }
        });*/
    }

    public static Socket getSocket() {
        return socket;
    }

    public static PrintWriter getServerOut() {
        return serverOut;
    }

    public static BufferedReader getServerIn() {
        return serverIn;
    }

    public static DataInputStream getDataInputStream() {
        return dataInputStream;
    }

}
