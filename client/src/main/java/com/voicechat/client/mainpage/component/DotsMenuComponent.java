package com.voicechat.client.mainpage.component;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.controller.HeaderController;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

public class DotsMenuComponent {

    public void setDotsMenu(ImageView threeDotsView, Scene scene, HeaderController headerController) {
        VBox verticalMenu = new VBox();
        Label settingsLabel = new Label("Settings");
        Label statusLabel = new Label("Status");
        verticalMenu.getChildren().addAll(settingsLabel, statusLabel);
        threeDotsView.setOnMouseClicked(event -> {
            User user = UserSession.INSTANCE.getUser();
            if (user.getSettings() != null) {
                if (user.getSettings().getThemeMode() == ThemeMode.DARK) {
                    scene.getStylesheets().remove(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                    scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
                    user.getSettings().setThemeMode(ThemeMode.LIGHT);
                }
                else {
                    scene.getStylesheets().remove(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
                    scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                    user.getSettings().setThemeMode(ThemeMode.DARK);
                }
            }
            else {
                scene.getStylesheets().remove(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
                scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                Settings settings = new Settings();
                settings.setThemeMode(ThemeMode.DARK);
                user.setSettings(settings);
            }
            headerController.changeTheme();
        });
        settingsLabel.setOnMouseClicked((event -> {
            scene.getStylesheets().remove(VoiceChatApplication.class
                    .getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
            scene.getStylesheets().add(VoiceChatApplication.class
                    .getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
        }));
    }

}
