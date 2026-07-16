package com.voicechat.client.mainpage.controller;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

public class LeftPaneController {

    @FXML
    private VBox chatVBox;

    @FXML
    private VBox calendarVBox;

    @FXML
    private VBox notificationVBox;

    @FXML
    public void initialize() {
        User user = UserSession.INSTANCE.getUser();
        String theme = null;
        if (user.getSettings() != null) {
            if (user.getSettings().getThemeMode() == ThemeMode.DARK) {
                theme = "/com/voicechat/client/css/dark-theme.css";
            }
            else {
                theme = "/com/voicechat/client/css/light-theme.css";
            }
        }
        else {
            theme = "/com/voicechat/client/css/light-theme.css";
        }
        String finalTheme = theme;
        calendarVBox.setOnMouseClicked(e -> {
            loadView("mainpage/calendar.fxml", finalTheme, e);
        });
        chatVBox.setOnMouseClicked(e -> {
            loadView("mainpage/main-page-view.fxml", finalTheme, e);
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
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                stage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
