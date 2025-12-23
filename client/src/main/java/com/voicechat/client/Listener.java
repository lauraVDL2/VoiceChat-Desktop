package com.voicechat.client;

import com.voicechat.client.login.controller.ConnectController;
import com.voicechat.client.login.controller.LoginController;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import java.io.*;

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

    private static BufferedReader userInput;

    private static ServerReader serverReader;

    private static int offset = 0;

    /**
     * Initialize connection and server reader
     */
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

        socketFuture.thenAcceptAsync(sock -> {
            try {
                userInput = new BufferedReader(new InputStreamReader(System.in));
                serverIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                serverOut = new PrintWriter(sock.getOutputStream(), true);
                dataInputStream = new DataInputStream(sock.getInputStream());

                // Initialize ServerReader with a broadcast handler if needed
                serverReader = new ServerReader(dataInputStream);
                serverReader.startReadingWithCorrelationId();

                // Register serverReader with this class for request-response handling
                // (Optionally, you can make serverReader static or manage differently)

                displayLogPanel(loginController, stage, loginRoot);

                // Further message handling...
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void displayLogPanel(LoginController loginController, Stage stage, Parent loginRoot) {
        Platform.runLater(() -> {
            if (loginController != null) {
                Scene scene = new Scene(loginRoot, 300, 300);
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

    public static BufferedReader getUserInput() {
        return userInput;
    }

    public static ServerReader getServerReader() {
        return serverReader;
    }

    public static int getOffset() {
        return offset;
    }

    public static int incrementOffset() {
        return ++offset;
    }

    public static int decrementOffset() {
        return --offset;
    }

    public static void setOffset(int offset) {
        Listener.offset = offset;
    }
}
