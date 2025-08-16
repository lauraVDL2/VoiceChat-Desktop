package com.voicechat.client;

import com.voicechat.client.login.ConnectController;
import com.voicechat.client.login.LoginController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class VoiceChatApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        exit(stage);

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

    }

    public void exit(Stage stage) {
        stage.setOnCloseRequest(windowEvent -> {
            Platform.exit();
            System.exit(0);
        });
    }
}
