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
        calendarVBox.setOnMouseClicked(e -> {
            loadView("mainpage/calendar.fxml", e);
        });
        chatVBox.setOnMouseClicked(e -> {
            loadView("mainpage/main-page-view.fxml", e);
        });
    }

    private void loadView(String fxmlFile,  Event event) {
        Platform.runLater(() -> {
            try {
                User user = UserSession.INSTANCE.getUser();
                String theme = null, toRemove = null;
                if (user.getSettings() != null) {
                    ThemeMode themeMode = user.getSettings().getThemeMode();
                    System.out.println("settings = " + user.getSettings().getThemeMode());
                    switch (themeMode) {
                        case LIGHT:
                            toRemove = "/com/voicechat/client/css/dark-theme.css";
                            theme = "/com/voicechat/client/css/light-theme.css";
                            break;
                        case DARK:
                            theme = "/com/voicechat/client/css/dark-theme.css";
                            toRemove = "/com/voicechat/client/css/light-theme.css";
                            break;
                    }
                }
                else {
                    toRemove = "/com/voicechat/client/css/dark-theme.css";
                    theme = "/com/voicechat/client/css/light-theme.css";
                }
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource(fxmlFile));
                Parent root = loader.load();
                Scene scene = new Scene(root, 300, 300);
                scene.getStylesheets().remove(VoiceChatApplication.class.getResource(toRemove).toExternalForm());
                scene.getStylesheets().add(VoiceChatApplication.class.getResource(theme).toExternalForm());
                //scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                stage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
