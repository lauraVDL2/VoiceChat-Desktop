package com.voicechat.test.calendar.controller;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.calendar.component.CalendarComponent;
import com.voicechat.client.calendar.controller.CalendarController;
import com.voicechat.client.calendar.service.CalendarService;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.shared.pojo.VoiceChatEvent;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class CalendarControllerTest extends FxRobot {
    private CalendarController calendarController;
    @Mock
    private CalendarService calendarService;
    @Mock
    private CalendarComponent calendarComponent;
    @Mock
    private PrintWriter out;
    @Mock
    private ServerReader serverReader;
    private VoiceChatEvent voiceChatEvent;
    private User user;

    @Start
    public void start(Stage stage) throws Exception {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setDisplayName("test");
        user.setEmailAddress("test.test@example.com");
        user.setUserName("test");
        UserSession.INSTANCE.setUser(user);

        // Load FXML and set controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/mainpage/calendar.fxml"));
        Parent root = loader.load();
        calendarController = loader.getController();

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, serverReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        Field calendarServiceField = CalendarController.class.getDeclaredField("calendarService");
        calendarServiceField.setAccessible(true);
        calendarServiceField.set(calendarController, calendarService);

        Field calendarComponentField = CalendarController.class.getDeclaredField("calendarComponent");
        calendarComponentField.setAccessible(true);
        calendarComponentField.set(calendarController, calendarComponent);

        voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setId("123");
        voiceChatEvent.setStart(LocalDateTime.now().toString());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void testDeleteEvent_successfulResponse() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        // Arrange
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseMessage(ServerResponseMessage.EVENT_DELETED);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        when(calendarService.deleteEvent(any(VoiceChatEvent.class))).thenReturn(serverResponse);

        CalendarController spyCalendarController = spy(calendarController);

        Platform.runLater(() -> {
            // Act
            spyCalendarController.deleteEvent(voiceChatEvent);

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

        // Assert
        assertEquals(user.getEmailAddress(), voiceChatEvent.getOrganizer());
        //verify(spyCalendarController).getEventsInWeek(any(LocalDate.class));
    }

    @Test
    void createEvent_successfulResponse() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch getEventsLatch = new CountDownLatch(1); // To wait for getEventsInWeek call

        // Mock server response
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseMessage(ServerResponseMessage.EVENT_CREATED);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        when(calendarService.createEvent(any(VoiceChatEvent.class))).thenReturn(serverResponse);

        // Spy on the calendarController to intercept getEventsInWeek calls
        CalendarController spyCalendarController = spy(calendarController);

        // Override getEventsInWeek to count down latch when called
        doAnswer(invocation -> {
            getEventsLatch.countDown();
            return invocation.callRealMethod();
        }).when(spyCalendarController).getEventsInWeek(any(LocalDate.class));

        // Run createEvent() inside Platform.runLater
        Platform.runLater(() -> {
            spyCalendarController.createEvent(voiceChatEvent);
            latch.countDown();
        });

        // Wait for createEvent() to be invoked
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for createEvent to be invoked");

        // Wait for getEventsInWeek() to be called
        assertTrue(getEventsLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for getEventsInWeek to be called");

        // Additional assertions
        verify(spyCalendarController).getEventsInWeek(any(LocalDate.class));
    }

}
