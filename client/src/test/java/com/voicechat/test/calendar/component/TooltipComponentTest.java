package com.voicechat.test.calendar.component;

import com.voicechat.client.calendar.component.TooltipComponent;
import com.voicechat.client.calendar.controller.CalendarController;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(ApplicationExtension.class)
public class TooltipComponentTest extends FxRobot {
    private TooltipComponent tooltipComponent;
    private Rectangle rectangle;

    @Start
    public void start(Stage stage) {
        MockitoAnnotations.openMocks(this);
        tooltipComponent = new TooltipComponent();
        // Create a root layout and add the rectangle
        StackPane root = new StackPane();
        rectangle = new Rectangle(100, 100);
        root.getChildren().add(rectangle);
        User user = new User();
        user.setEmailAddress("toto.toto@yahoo.fr");
        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.LIGHT);
        user.setSettings(settings);
        UserSession.INSTANCE.setUser(user);
        Scene scene = new Scene(root, 400, 400);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testInitCreateMeetingTooltip() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String currentDay = LocalDate.now().toString();
        int row = 1;
        CalendarController calendarController = mock(CalendarController.class);

        // Call initCreateMeetingTooltip
        Platform.runLater(() -> {
            // Initialize tooltip and set event handler
            tooltipComponent.initCreateMeetingTooltip(rectangle, currentDay, row, calendarController);
            latch.countDown(); // Signal that init is done
        });

        // Wait until initCreateMeetingTooltip has run and attached handler
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Timeout waiting for initCreateMeetingTooltip to finish");

        // Fire the mouse click event
        Platform.runLater(() -> {
            MouseEvent mouseEvent = new MouseEvent(MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, null, 1, false, false, false, false,
                    false, false, false, false, false,
                    false, null);
            rectangle.fireEvent(mouseEvent);
        });

        Thread.sleep(500);

        // Check that the fill color has been updated
        assertEquals(new Color(0.27, 0.51, 0.70, 1), rectangle.getFill(),
                "Rectangle fill color should be updated to new color");
    }

}
