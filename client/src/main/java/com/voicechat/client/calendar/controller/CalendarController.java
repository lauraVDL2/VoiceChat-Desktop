package com.voicechat.client.calendar.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.calendar.component.CalendarComponent;
import com.voicechat.client.calendar.service.CalendarService;
import com.voicechat.client.login.UserSession;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.shared.JsonMapper;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.mapped_entity.VoiceChatCalendar;
import org.shared.mapped_entity.VoiceChatEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CalendarController {

    @FXML
    private VBox includedLeftPane;

    @FXML
    private GridPane gridCalendarPane;

    private final CalendarComponent calendarComponent = new CalendarComponent();

    private final CalendarService calendarService = new CalendarService();

    @FXML
    public void initialize() {
        initializeLeftPaneButton();
        getEventsInWeek();
    }

    public void initializeLeftPaneButton() {
        VBox calendarVBox = (VBox) includedLeftPane.lookup("#calendarVBox");
        var nodes = includedLeftPane.lookupAll(".leftPaneButtonClicked");
        for (var node : nodes) {
            node.getStyleClass().remove("leftPaneButtonClicked");
        }
        calendarVBox.getStyleClass().add("leftPaneButtonClicked");
    }

    public void getEventsInWeek() {
        CompletableFuture.supplyAsync(() -> {
            try {
                VoiceChatCalendar voiceChatCalendar = new VoiceChatCalendar();
                voiceChatCalendar.setStartWeekTime("2025-02-03T00:00:00-08:00");
                voiceChatCalendar.setEndWeekTime("2025-02-07T23:59:00-08:00");
                voiceChatCalendar.setOwnerEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());
                return calendarService.getEvents(voiceChatCalendar);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(serverResponse -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.EVENTS_GET) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        try {
                            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                            List<VoiceChatEvent> events = objectMapper.readValue(serverResponse.getBinaryPayload(),
                                    new TypeReference<List<VoiceChatEvent>>(){});
                            calendarComponent.setCalendar(gridCalendarPane, events);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
