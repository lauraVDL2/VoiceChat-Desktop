package com.voicechat.client.login.controller;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.login.service.RegisterService;
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
import org.shared.*;
import org.shared.entity.User;

import java.io.IOException;
import java.util.UUID;

public class RegisterController {

    @FXML
    private Label emailAddressLabel;

    @FXML
    private TextField emailAddressField;

    @FXML
    private Label passwordLabel;

    @FXML
    private TextField passwordField;

    @FXML
    private Label displayedNameLabel;

    @FXML
    private TextField displayedNameField;

    @FXML
    private Label switchLoginLabel;

    @FXML
    private Button registerButton;

    @FXML
    private Label errorEmail;
    @FXML
    private Label errorMessageLog;

    private final RegisterService registerService = new RegisterService();

    private final ServerReader serverReader = Listener.getServerReader();

    private final ConnectController connectController = new ConnectController();

    @FXML
    public void initialize() {
        serverReader.startReadingWithCorrelationId();
        switchLoginView();
        registerUser();
    }

    public void onConnected() {
        Platform.runLater(() -> {
            emailAddressLabel.setText(emailAddressLabel.getText().toUpperCase());
            passwordLabel.setText(passwordLabel.getText().toUpperCase());
            displayedNameLabel.setText(displayedNameLabel.getText().toUpperCase());
        });
    }

    public void registerUser() {
        registerButton.setOnMouseClicked(event -> {
            String email = emailAddressField.getText();
            String password = passwordField.getText();
            String displayedName = displayedNameField.getText();

            if (!registerService.verifyEmail(email)) {
                emailAddressField.setStyle("-fx-border-color: #af2e2e;");
                errorEmail.setVisible(true);
                return;
            } else {
                emailAddressField.setStyle(null);
                errorEmail.setVisible(false);
            }

            // Synchronously call the registration service (blocking)
            ServerResponse serverResponse = null;
            try {
                User user = new User(email, displayedName, password);
                String correlationId = UUID.randomUUID().toString();
                registerService.register(user, correlationId);
                serverResponse = serverReader.getServerResponseByCorrelationId(correlationId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Handle the response FIRST, before switching scenes
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_CREATED) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.FAILURE) {
                        errorMessageLog.setText(serverResponse.getMessage());
                        errorMessageLog.setVisible(true);
                        return;  // ← Exit early on failure, don't switch scenes
                    } else {
                        UserSession.INSTANCE.setUser(new User(email, displayedName, password));
                        // Fall through to switch scene on success
                    }
                }
            }

            // Now switch scene (only for success or if needed)
            try {
                FXMLLoader mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("login/connect-view.fxml"));
                Parent root = mainPageLoader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 300, 300);
                stage.setScene(scene);
                connectController.loadUserScreen(serverResponse, stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void switchMainPage(Stage stage) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loginLoader = new FXMLLoader(VoiceChatApplication.class.getResource("main-page/main-page-view.fxml"));
                Parent root = loginLoader.load();

                Scene scene = new Scene(root, 300, 300);
                stage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void switchLoginView() {
        switchLoginLabel.setOnMouseClicked(event -> {
            try {
                FXMLLoader loginLoader = new FXMLLoader(VoiceChatApplication.class.getResource("login/login-view.fxml"));
                Parent root = loginLoader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 300, 300);
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/login.css").toExternalForm());
                stage.setScene(scene);

                LoginController loginController = loginLoader.getController();
                loginController.onConnected();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
