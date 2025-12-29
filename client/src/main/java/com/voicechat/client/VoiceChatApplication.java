package com.voicechat.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.login.controller.ConnectController;
import com.voicechat.client.login.controller.LoginController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;

public class VoiceChatApplication extends Application {

    private static HostServices hostServices;

    @Override
    public void start(Stage stage) throws IOException {
        hostServices = this.getHostServices();

        Image icon = new Image(getClass().getResourceAsStream("/com/voicechat/client/images/voiceCallIcon.png"));

        // Set the icon for the stage
        stage.getIcons().add(icon);

        FXMLLoader connectLoader = new FXMLLoader(getClass().getResource("login/connect-view.fxml"));
        Parent connectRoot = connectLoader.load();
        ConnectController connectController = connectLoader.getController();

        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login/login-view.fxml"));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();

        stage.setMaximized(true);
        Scene connectScene = new Scene(connectRoot, 320, 240);
        stage.setTitle("VoiceChat Desktop");
        stage.setScene(connectScene);
        stage.show();
        Listener.connect(loginController, connectController, stage, loginRoot);

        exit(stage);
    }

    public void exit(Stage stage) {
        stage.setOnCloseRequest(windowEvent -> {
            try {
                closeSession();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    public void closeSession() throws JsonProcessingException {
        User user = UserSession.INSTANCE.getUser();
        if (user != null) {
            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
            String json = objectMapper.writeValueAsString(user);
            Message message = new Message(MessageType.USER_EXIT, json);
            PrintWriter serverOut = Listener.getServerOut();

            serverOut.println(objectMapper.writeValueAsString(message));

            UserSession.INSTANCE.clear();
        }
    }

    public static HostServices getBrowserServices() {
        return hostServices;
    }
}
