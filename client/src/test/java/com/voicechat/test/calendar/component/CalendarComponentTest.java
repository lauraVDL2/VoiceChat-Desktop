package com.voicechat.test.calendar.component;

import com.voicechat.client.calendar.component.CalendarComponent;
import com.voicechat.client.calendar.component.DatePickerComponent;
import com.voicechat.client.calendar.component.TooltipComponent;
import com.voicechat.client.calendar.controller.CalendarController;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;
import org.shared.pojo.VoiceChatEvent;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class CalendarComponentTest extends FxRobot {
    private CalendarComponent calendarComponent;
    @Mock
    private DatePickerComponent datePickerComponent;
    @Mock
    private TooltipComponent tooltipComponent;
    private VoiceChatEvent voiceChatEvent;

    @BeforeEach
    public void setup() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        calendarComponent = new CalendarComponent();

        Field tooltipComponentField = CalendarComponent.class.getDeclaredField("tooltipComponent");
        tooltipComponentField.setAccessible(true);
        tooltipComponentField.set(calendarComponent, tooltipComponent);

        Field datePickerComponentField = CalendarComponent.class.getDeclaredField("datePickerComponent");
        datePickerComponentField.setAccessible(true);
        datePickerComponentField.set(calendarComponent, datePickerComponent);

        User user = new User();
        user.setEmailAddress("toto.toto@yahoo.fr");
        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.LIGHT);
        user.setSettings(settings);
        UserSession.INSTANCE.setUser(user);

        voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setId("123");
        voiceChatEvent.setStart(LocalDateTime.now().toString());
        voiceChatEvent.setEnd(LocalDateTime.now().plusHours(1).toString());
        voiceChatEvent.setOnlineMeeting(true);
    }

    @Test
    void testSetCalendar() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        GridPane rootPane = new GridPane();
        rootPane.getChildren().add(new VBox());


        List<VoiceChatEvent> events = List.of(voiceChatEvent);
        String currentDate = voiceChatEvent.getStart();
        CalendarController calendarController = mock(CalendarController.class);

        Platform.runLater(() -> {
            calendarComponent.setCalendar(rootPane, events, currentDate, calendarController);
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

        assertNotNull(rootPane);
        assertFalse(rootPane.getChildren().isEmpty());
    }
}
