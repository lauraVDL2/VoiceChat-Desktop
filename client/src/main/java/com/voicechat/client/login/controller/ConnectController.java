package com.voicechat.client.login.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.IOException;

public class ConnectController {

    /*@FXML
    private Label messageLabel;

    // Call this method when connected
    public void showConnecting() {
        // Update UI components safely on JavaFX Application Thread
        Platform.runLater(() -> {
            messageLabel.setText("Trying to connect");
        });
    }

    public void showConnectionFailed() {
        Platform.runLater(() -> {
            messageLabel.setText("Connection failed...");
        });
    }*/

    public static void loadUserScreen(ServerResponse serverResponse, Stage stage) throws IOException {
        if (serverResponse != null) {
            if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                FXMLLoader mainPageLoader = null;
                Parent root = null;
                Scene scene = null;
                switch (serverResponse.getServerResponseMessage()) {
                    case USER_LOGGED_IN:
                        String payload = serverResponse.getPayload();
                        ObjectMapper mapper = new ObjectMapper();
                        User loggedUser = mapper.readValue(payload, User.class);
                        UserSession.INSTANCE.setUser(loggedUser);
                        // Load main page
                        mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/main-page-view.fxml"));
                        root = mainPageLoader.load();
                        scene = new Scene(root, 300, 300);
                        stage.setScene(scene);
                        break;
                    case USER_CREATED:
                        // Load main page
                        mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/main-page-view.fxml"));
                        root = mainPageLoader.load();
                        scene = new Scene(root, 300, 300);
                        stage.setScene(scene);
                        break;
                }
            }
        }
    }

}
