package com.voicechat.client.common.utils;

import com.voicechat.client.common.UserSession;
import javafx.scene.paint.Color;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

public class ColorUtil {

    public static Color getRectangleBorderColor() {
        User user = UserSession.INSTANCE.getUser();
        Settings settings = user.getSettings();
        Color color = Color.LIGHTGRAY;
        if (settings != null) {
            ThemeMode themeMode = settings.getThemeMode();
            if (themeMode == ThemeMode.DARK) {
                color = Color.DARKGRAY;
            }
        }
        return color;
    }

    public static Color getRectangleColor() {
        User user = UserSession.INSTANCE.getUser();
        Settings settings = user.getSettings();
        Color color = Color.LIGHTGRAY;
        if (settings != null) {
            ThemeMode themeMode = settings.getThemeMode();
            if (themeMode == ThemeMode.DARK) {
                color = Color.web("#393a41");
            }
        }
        return color;
    }

    public static Color getSelectedRectangleColor() {
        User user = UserSession.INSTANCE.getUser();
        Settings settings = user.getSettings();
        Color color = new Color(0.27, 0.51, 0.70, 1);
        if (settings != null) {
            ThemeMode themeMode = settings.getThemeMode();
            if (themeMode == ThemeMode.DARK) {
                color = new Color(0.545, 0.267, 0.565, 1);
            }
        }
        return color;
    }

    public static Color getMeetingColor() {
        User user = UserSession.INSTANCE.getUser();
        Settings settings = user.getSettings();
        Color color = new Color(0.27, 0.51, 0.70, 0.8);
        if (settings != null) {
            ThemeMode themeMode = settings.getThemeMode();
            if (themeMode == ThemeMode.DARK) {
                color = new Color(0.545, 0.267, 0.565, 0.8);
            }
        }
        return color;
    }
}
