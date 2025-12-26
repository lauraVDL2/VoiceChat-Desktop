package com.voicechat.client.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.login.component.AuthenticatePopupComponent;
import com.voicechat.client.login.service.ConnectService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.shared.*;
import org.shared.entity.User;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConnectController {

    private static final ConnectService connectService = new ConnectService();

    public void loadUserScreen(ServerResponse serverResponse, Stage stage) {
        if (serverResponse != null && serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_LOGGED_IN) {
                        ObjectMapper mapper = JsonMapper.getJsonMapper();
                        byte[] payload = serverResponse.getBinaryPayload();
                        User loggedUser = mapper.readValue(payload, User.class);
                        UserSession.INSTANCE.setUser(loggedUser);
                        microsoftAuthenticate(stage);
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

    public static void microsoftAuthenticate(Stage stage) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return connectService.getMicrosoftAuthentication();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync((serverResponse) -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MICROSOFT_AUTHENTICATED) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        ObjectMapper mapper = JsonMapper.getJsonMapper();
                        Authenticate authenticate = null;
                        try {
                            authenticate = mapper.readValue(serverResponse.getBinaryPayload(), Authenticate.class);
                            AuthenticatePopupComponent.showPopup(stage, authenticate.getCode());
                            VoiceChatApplication.getBrowserServices().showDocument(authenticate.getUrl());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

}
