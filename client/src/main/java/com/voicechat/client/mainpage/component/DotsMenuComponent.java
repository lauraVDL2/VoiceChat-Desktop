package com.voicechat.client.mainpage.component;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.controller.HeaderController;
import com.voicechat.client.settings.controller.SettingsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

public class DotsMenuComponent {

    public void setDotsMenu(ImageView threeDotsView, Pane menuPane,
                            Scene scene, HeaderController headerController) {
        VBox verticalMenu = new VBox();
        verticalMenu.getStyleClass().add("dotsVerticalMenu");
        Label settingsLabel = new Label("Settings");
        Label statusLabel = new Label("Status");
        verticalMenu.getChildren().addAll(settingsLabel, statusLabel);
        threeDotsView.setOnMouseClicked((event) -> {
            menuPane.getChildren().clear();
            menuPane.getChildren().add(verticalMenu);
            settingsLabel.setOnMouseClicked(event1 -> {
                try {
                    FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource("settings/settings.fxml"));
                    Parent childNode = loader.load();
                    scene.setRoot(childNode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

}
