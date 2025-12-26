package com.voicechat.client.login.controller;

import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
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
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    private final ConnectController connectController = new ConnectController();

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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();


            // Run login process asynchronously
            CompletableFuture.supplyAsync(() -> {
                User user = new User(email, password);
                try {
                    String correlationId = UUID.randomUUID().toString();
                    loginService.login(user, correlationId);
                    return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }).thenAcceptAsync(serverResponse -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.FAILURE) {
                        // Show error message on JavaFX thread
                        Platform.runLater(() -> {
                            errorMessageLog.setVisible(true);
                            errorMessageLog.setText(serverResponse.getMessage());
                        });
                    } else {
                        // Successful login, load main page
                        Platform.runLater(() -> {
                            try {
                                FXMLLoader mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("login/connect-view.fxml"));
                                Parent root = mainPageLoader.load();
                                Scene scene = new Scene(root, 300, 300);
                                stage.setScene(scene);
                                connectController.loadUserScreen(serverResponse, stage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else {
                    // Handle null response if needed
                    Platform.runLater(() -> {
                        errorMessageLog.setVisible(true);
                        errorMessageLog.setText("Login failed due to an error.");
                    });
                }
            });
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
