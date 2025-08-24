package com.voicechat.client.login.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.utils.JsonMapper;
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
import java.util.concurrent.CompletableFuture;

public class ConnectController {

    public static void loadUserScreen(ServerResponse serverResponse, Stage stage) {
        if (serverResponse != null && serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    // Deserialize payload if needed
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_LOGGED_IN) {
                        String payload = serverResponse.getPayload();
                        ObjectMapper mapper = JsonMapper.getJsonMapper();
                        User loggedUser = mapper.readValue(payload, User.class);
                        UserSession.INSTANCE.setUser(loggedUser);
                    }
                    // Load FXML
                    FXMLLoader mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/main-page-view.fxml"));
                    Parent root = mainPageLoader.load();
                    return root;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).thenAcceptAsync(root -> {
                if (root != null) {
                    Scene scene = new Scene(root, 300, 300);
                    scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/main-page.css").toExternalForm());
                    Platform.runLater(() -> {
                        stage.setScene(scene);
                    });
                }
            });
        }
    }

}
