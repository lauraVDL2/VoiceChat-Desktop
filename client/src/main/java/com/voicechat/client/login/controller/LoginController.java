package com.voicechat.client.login.controller;

import com.voicechat.client.VoiceChatApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    private Label loginText;
    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameField;
    @FXML
    private Label emailAddressLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private Label switchRegisterLabel;

    @FXML
    public void initialize() {
        switchRegisterView();
    }

    // Call this method when connected
    public void onConnected() {
        // Update UI components safely on JavaFX Application Thread
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            usernameField.setDisable(false);
            emailAddressLabel.setText(emailAddressLabel.getText().toUpperCase());
            passwordLabel.setText(passwordLabel.getText().toUpperCase());
        });
    }

    public void switchRegisterView() {
        switchRegisterLabel.setOnMouseClicked(event -> {
            try {
                FXMLLoader registerLoader = new FXMLLoader(VoiceChatApplication.class.getResource("login/register-view.fxml"));
                Parent root = registerLoader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 300, 300);
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/login.css").toExternalForm());
                stage.setScene(scene);

                RegisterController registerController = registerLoader.getController();
                registerController.onConnected();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
