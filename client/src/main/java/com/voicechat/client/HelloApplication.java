package com.voicechat.client;

import com.voicechat.client.login.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        // Load login view
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        Parent loginRoot = loginLoader.load(); // Load FXML first
        LoginController loginController = loginLoader.getController(); // Then get the controller

        Listener.connect(loginController);

        Scene scene2 = new Scene(loginRoot, 320, 240);
        stage.setTitle("Login");
        stage.setScene(scene2);
        stage.show();
    }
}
