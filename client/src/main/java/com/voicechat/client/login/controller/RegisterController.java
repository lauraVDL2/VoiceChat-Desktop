package com.voicechat.client.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final RegisterService registerService = new RegisterService();

    @FXML
    public void initialize() {
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
                serverResponse = registerService.register(user);
            } catch (IOException e) {
                e.printStackTrace();
                // Optionally show an error alert here
            }

            // Handle the response directly on the JavaFX thread
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_CREATED) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        UserSession.INSTANCE.setUser(new User(email, displayedName, password));
                        try {
                            // Load main page
                            FXMLLoader mainPageLoader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/main-page-view.fxml"));
                            Parent root = mainPageLoader.load();
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            Scene scene = new Scene(root, 300, 300);
                            stage.setScene(scene);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    else {
                        return;
                    }
                }
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
