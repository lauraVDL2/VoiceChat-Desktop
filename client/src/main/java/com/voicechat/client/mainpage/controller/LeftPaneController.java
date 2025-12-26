package com.voicechat.client.mainpage.controller;

import com.voicechat.client.VoiceChatApplication;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LeftPaneController {

    @FXML
    private VBox chatVBox;

    @FXML
    private VBox calendarVBox;

    @FXML
    private VBox notificationVBox;

    @FXML
    public void initialize() {
        calendarVBox.setOnMouseClicked(e -> {
            loadView("mainpage/calendar.fxml", "/com/voicechat/client/css/calendar.css", e);
        });
        chatVBox.setOnMouseClicked(e -> {
            loadView("mainpage/main-page-view.fxml", "/com/voicechat/client/css/main-page.css", e);
        });
    }

    private void loadView(String fxmlFile, String cssFile, Event event) {
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource(fxmlFile));
                Parent root = loader.load();
                Scene scene = new Scene(root, 300, 300);
                scene.getStylesheets().add(VoiceChatApplication.class.getResource(cssFile).toExternalForm());
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/left-pane.css").toExternalForm());
                stage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
