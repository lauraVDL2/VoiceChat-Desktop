package com.voicechat.test.settings.component;

import com.voicechat.client.common.UserSession;
import com.voicechat.client.settings.component.GeneralTabComponent;
import com.voicechat.client.settings.controller.SettingsController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class GeneralTabComponentTest extends FxRobot {
    private GeneralTabComponent generalTabComponent;
    private User user;
    private ChoiceBox<String> choiceBox;
    @Mock
    private ObservableList<String> style;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        generalTabComponent = new GeneralTabComponent();
        choiceBox = new ChoiceBox<>();
        var items = FXCollections.observableArrayList(
                "LIGHT",
                "DARK"
        );
        choiceBox.setItems(items);

        user = new User();
        user.setEmailAddress("toto.toto@yahoo.fr");
        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.LIGHT);
        user.setSettings(settings);
        UserSession.INSTANCE.setUser(user);
    }

    @Test
    void testSetDefaultThemeValue() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            generalTabComponent.setDefaultThemeValue(choiceBox);
            latch.countDown();
        });

        boolean scheduled = latch.await(5, TimeUnit.SECONDS);
        assertTrue(scheduled, "Timeout waiting for method to be called");

        assertNotNull(choiceBox.getValue());
        assertEquals(choiceBox.getValue(), user.getSettings().getThemeMode().name());
    }

    @Test
    void testChangeTheme() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Scene scene = mock(Scene.class);
        SettingsController settingsController = mock(SettingsController.class);
        when(scene.getStylesheets()).thenReturn(style);

        var spyBox = spy(choiceBox);
        var spyBoxSelection = spy(choiceBox.getSelectionModel());
        when(spyBox.getSelectionModel()).thenReturn(spyBoxSelection);

        Platform.runLater(() -> {
            generalTabComponent.changeTheme(spyBox, scene, settingsController);
            spyBoxSelection.select("DARK");
            latch.countDown();
        });

        latch.await();

        // Verify that the select event is indeed called
        verify(spyBoxSelection, times(1)).select(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(String argument) {
                return argument.equals("DARK");
            }
        }));
        assertEquals(ThemeMode.DARK, user.getSettings().getThemeMode());
    }
}
