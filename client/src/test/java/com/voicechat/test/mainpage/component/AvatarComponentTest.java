package com.voicechat.test.mainpage.component;

import com.voicechat.client.mainpage.component.AvatarComponent;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
public class AvatarComponentTest extends FxRobot {
    private AvatarComponent avatarComponent;

    @BeforeEach
    public void setup() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        avatarComponent = new AvatarComponent();
    }

    @Test
    void testOnlineCircle() throws Exception {
        StackPane stackPane = new StackPane();
        Color color = new Color(0, 0, 0, 1);

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            avatarComponent.onlineCircle(stackPane, color);
            latch.countDown();
        });

        boolean scheduled = latch.await(5, TimeUnit.SECONDS);
        assertTrue(scheduled, "Timeout waiting for addMessageBox to be called");

        // Now, wait again for the UI updates to complete
        CountDownLatch updateLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Here, perform any additional checks or just signal completion
            updateLatch.countDown();
        });
        assertTrue(updateLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for UI update");

        assertNotNull(stackPane);
        assertFalse(stackPane.getChildren().isEmpty());
    }

}
