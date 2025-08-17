package com.voicechat.client.login.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ConnectController {

    @FXML
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
    }
}
