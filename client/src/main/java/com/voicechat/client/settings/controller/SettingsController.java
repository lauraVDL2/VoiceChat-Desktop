package com.voicechat.client.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.settings.service.SettingsService;
import com.voicechat.client.settings.component.GeneralTabComponent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import org.shared.JsonMapper;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

import java.util.concurrent.CompletableFuture;

public class SettingsController {
    @FXML
    private GridPane settingsMainPane;
    @FXML
    private ChoiceBox<String> choiceBoxTheme;

    private final SettingsService settingsService = new SettingsService();

    private final GeneralTabComponent generalTabComponent = new GeneralTabComponent();

    @FXML
    public void initialize() {
        var nodes = settingsMainPane.lookupAll(".leftPaneButtonClicked");
        for (var node : nodes) {
            node.getStyleClass().remove("leftPaneButtonClicked");
        }
        generalTabComponent.setDefaultThemeValue(choiceBoxTheme);
        choiceBoxTheme.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                generalTabComponent.changeTheme(choiceBoxTheme, newScene, this);
            }
        });
    }

    public void changeTheme(ChoiceBox<String> choiceBox) {
        CompletableFuture.supplyAsync(() -> {
            try {
                User user = UserSession.INSTANCE.getUser();
                Settings sendTheme = user.getSettings();
                if (sendTheme != null) {
                    switch (choiceBox.getValue()) {
                        case "LIGHT":
                            sendTheme.setThemeMode(ThemeMode.LIGHT);
                            break;
                        case "DARK":
                            sendTheme.setThemeMode(ThemeMode.DARK);
                            break;
                    }
                    user.getSettings().setThemeMode(sendTheme.getThemeMode());
                    return settingsService.setThemeMode(user);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(serverResponse -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.THEME_MODE_SET) {
                        try {
                            ObjectMapper jsonMapper = JsonMapper.getJsonMapper();
                            Settings settings = jsonMapper.readValue(serverResponse.getBinaryPayload(), Settings.class);
                            System.out.println(settings.getThemeMode() + " settings theme mode");
                            UserSession.INSTANCE.getUser().setSettings(settings);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, Platform::runLater)
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
