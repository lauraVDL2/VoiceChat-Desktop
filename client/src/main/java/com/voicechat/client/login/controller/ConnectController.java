package com.voicechat.client.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.login.component.AuthenticatePopupComponent;
import com.voicechat.client.login.service.ConnectService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.shared.*;
import org.shared.entity.User;
import org.shared.pojo.MicrosoftAccount;

import java.io.IOException;
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
                    scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/left-pane.css").toExternalForm());
                    Platform.runLater(() -> {
                        stage.setScene(scene);
                    });
                }
            });
        }
    }

    public void microsoftAuthenticate(Stage stage) {
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
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.INFO) {
                        ObjectMapper mapper = JsonMapper.getJsonMapper();
                        Authenticate authenticate = null;
                        try {
                            authenticate = mapper.readValue(serverResponse.getBinaryPayload(), Authenticate.class);
                            AuthenticatePopupComponent.showPopup(stage, authenticate.getCode());
                            VoiceChatApplication.getBrowserServices().showDocument(authenticate.getUrl());
                            setMicrosoftUserSession(serverResponse, mapper);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void setMicrosoftUserSession(ServerResponse serverResponse, ObjectMapper objectMapper) throws InterruptedException, IOException {
        ServerResponse response = Listener.getServerReader()
                .getServerResponseByCorrelationId("msa-" + serverResponse.getCorrelationId());
        if (response != null) {
            if (response.getServerResponseMessage() == ServerResponseMessage.MICROSOFT_AUTHENTICATED) {
                if (response.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    MicrosoftAccount account = objectMapper.readValue(response.getBinaryPayload(), MicrosoftAccount.class);
                    UserSession.INSTANCE.setMicrosoftAccount(account);
                    System.out.println("account = " + account.getEmailAddress());
                }
            }
        }
    }

}
