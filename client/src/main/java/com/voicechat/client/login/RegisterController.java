package com.voicechat.client.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
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
import org.shared.Message;
import org.shared.MessageType;
import org.shared.User;

import java.io.IOException;
import java.io.PrintWriter;

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
            try {
                String email = emailAddressField.getText();
                String password = passwordField.getText();
                String displayedName = displayedNameField.getText();

                User user = new User(email, displayedName, password);


                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(user);

                Message message = new Message(MessageType.USER, json);

                PrintWriter serverOut = Listener.getServerOut();

                serverOut.println(mapper.writeValueAsString(message));
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
