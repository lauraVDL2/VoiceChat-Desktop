package com.voicechat.client.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.login.service.LoginService;
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
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.IOException;

public class LoginController {
    @FXML
    private Label loginText;
    @FXML
    private Button loginButton;
    @FXML
    private TextField emailAddressField;
    @FXML
    private TextField passwordField;
    @FXML
    private Label emailAddressLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private Label switchRegisterLabel;
    @FXML
    private Label errorMessageLog;

    private final LoginService loginService = new LoginService();

    @FXML
    public void initialize() {
        switchRegisterView();
        logUserIn();
    }

    // Call this method when connected
    public void onConnected() {
        // Update UI components safely on JavaFX Application Thread
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            emailAddressField.setDisable(false);
            emailAddressLabel.setText(emailAddressLabel.getText().toUpperCase());
            passwordLabel.setText(passwordLabel.getText().toUpperCase());
        });
    }

    public void logUserIn() {
        loginButton.setOnMouseClicked(event -> {
            String email = emailAddressField.getText();
            String password = passwordField.getText();

            ServerResponse serverResponse = null;
            try {
                FXMLLoader mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("login/connect-view.fxml"));
                Parent root = mainPageLoader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 300, 300);
                stage.setScene(scene);
                User user = new User(email, password);
                serverResponse = loginService.login(user);
                ConnectController.loadUserScreen(serverResponse, stage);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (serverResponse != null) {
                if(serverResponse.getServerResponseStatus() == ServerResponseStatus.FAILURE) {
                    errorMessageLog.setVisible(true);
                    errorMessageLog.setText(serverResponse.getMessage());
                }
            }
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
