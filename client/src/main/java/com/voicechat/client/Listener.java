package com.voicechat.client;

import com.voicechat.client.login.LoginController;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    public Listener() {

    }

    public static void connect(LoginController loginController) {
        // Wrap connection logic in a supplier
        Supplier<Socket> socketSupplier = () -> {
            System.out.println("Trying to connect to server...");
            try {
                return new Socket(SERVER_HOST, SERVER_PORT);
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
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);

                // Notify UI that connection is successful
                Platform.runLater(() -> {
                    if (loginController != null) {
                        loginController.onConnected();
                    }
                });

                // Asynchronously read messages from server
                CompletableFuture.runAsync(() -> {
                    String messageFromServer;
                    try {
                        while ((messageFromServer = serverIn.readLine()) != null) {
                            System.out.println("Server: " + messageFromServer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, executor);

                // Send user input to server
                String userMessage;
                while ((userMessage = userInput.readLine()) != null) {
                    serverOut.println(userMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor).join(); // Wait for completion or keep the app alive
    }
}
