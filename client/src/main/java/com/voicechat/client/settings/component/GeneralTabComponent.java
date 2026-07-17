package com.voicechat.client.settings.component;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.settings.controller.SettingsController;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import org.apache.commons.lang3.StringUtils;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

public class GeneralTabComponent {

    public void setDefaultThemeValue(ChoiceBox<String> choiceBox) {
        User user = UserSession.INSTANCE.getUser();
        Settings settings = user.getSettings();
        if (settings != null) {
            ThemeMode currentTheme = settings.getThemeMode();
            choiceBox.setValue(currentTheme.name());
        }
        else {
            choiceBox.setValue("LIGHT");
        }
    }

    public void changeTheme(ChoiceBox<String> choiceBox, Scene scene, SettingsController settingsController) {
        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            User user = UserSession.INSTANCE.getUser();
            if (user.getSettings() != null) {
                System.out.println("new value = " + newValue);
                if (StringUtils.equalsIgnoreCase(newValue, "LIGHT")) {
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
            settingsController.changeTheme();
        });
    }
}
