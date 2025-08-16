package com.voicechat.client.login;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private Label loginText;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameField;

    @FXML
    protected void onHelloButtonClick() {
        loginText.setText("Toto!");
    }

    // Call this method when connected
    public void onConnected() {
        // Update UI components safely on JavaFX Application Thread
        Platform.runLater(() -> {
            statusLabel.setText("Connected to server!");
            loginButton.setDisable(false);
            usernameField.setDisable(false);
            // Any other UI updates you want
        });
    }
}
