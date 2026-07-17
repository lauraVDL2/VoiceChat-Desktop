package com.voicechat.test.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.settings.component.GeneralTabComponent;
import com.voicechat.client.settings.controller.SettingsController;
import com.voicechat.client.settings.service.SettingsService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class SettingsControllerTest extends FxRobot {
    private SettingsController settingsController;
    @Mock
    private SettingsService settingsService;
    @Mock
    private GeneralTabComponent generalTabComponent;
    @Mock
    private PrintWriter out;
    @Mock
    private ServerReader serverReader;
    private User user;

    @Start
    public void start(Stage stage) throws Exception {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setDisplayName("test");
        user.setEmailAddress("test.test@example.com");
        user.setUserName("test");
        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.DARK);
        user.setSettings(settings);
        UserSession.INSTANCE.setUser(user);

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, serverReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/settings/settings.fxml"));
        Parent root = loader.load();
        settingsController = loader.getController();

        Field settingsServiceField = SettingsController.class.getDeclaredField("settingsService");
        settingsServiceField.setAccessible(true);
        settingsServiceField.set(settingsController, settingsService);

        Field generalTabComponentField = SettingsController.class.getDeclaredField("generalTabComponent");
        generalTabComponentField.setAccessible(true);
        generalTabComponentField.set(settingsController, generalTabComponent);

        when(serverReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void changeTheme() throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        CountDownLatch latch = new CountDownLatch(1);

        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseMessage(ServerResponseMessage.THEME_MODE_SET);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.DARK);
        serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(settings));

        when(settingsService.setThemeMode(any(User.class))).thenReturn(serverResponse);

        Platform.runLater(() -> {
            settingsController.changeTheme();
            latch.countDown();
        });

        boolean scheduled = latch.await(5, TimeUnit.SECONDS);
        assertTrue(scheduled, "Timeout waiting for method to be called");

        assertNotNull(user.getSettings());
        assertEquals(ThemeMode.DARK, user.getSettings().getThemeMode());
    }

}
