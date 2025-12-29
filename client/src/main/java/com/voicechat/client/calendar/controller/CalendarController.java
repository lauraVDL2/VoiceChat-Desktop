package com.voicechat.client.calendar.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.calendar.component.CalendarComponent;
import com.voicechat.client.calendar.service.CalendarService;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.common.utils.DateHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.shared.JsonMapper;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.pojo.VoiceChatCalendar;
import org.shared.pojo.VoiceChatEvent;

import java.time.*;
import java.time.format.DateTimeFormatter;
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
        getEventsInCurrentWeek(this);
    }

    public void initializeLeftPaneButton() {
        VBox calendarVBox = (VBox) includedLeftPane.lookup("#calendarVBox");
        var nodes = includedLeftPane.lookupAll(".leftPaneButtonClicked");
        for (var node : nodes) {
            node.getStyleClass().remove("leftPaneButtonClicked");
        }
        calendarVBox.getStyleClass().add("leftPaneButtonClicked");
    }

    public void deleteEvent(VoiceChatEvent voiceChatEvent) {
        CompletableFuture.supplyAsync(() -> {
            try {
                voiceChatEvent.setOrganizer(UserSession.INSTANCE.getUser().getEmailAddress());
                return calendarService.deleteEvent(voiceChatEvent);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(serverResponse -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.EVENT_DELETED) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        Platform.runLater(() -> {
                            LocalDateTime dateTime = LocalDateTime.parse(voiceChatEvent.getStart());
                            LocalDate date = dateTime.toLocalDate();
                            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                            this.getEventsInWeek(LocalDate.parse(formattedDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        });
                    }
                }
            }
        });
    }

    public void createEvent(VoiceChatEvent voiceChatEvent) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return calendarService.createEvent(voiceChatEvent);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(serverResponse -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.EVENT_CREATED) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        Platform.runLater(() -> {
                            LocalDateTime dateTime = LocalDateTime.parse(voiceChatEvent.getStart());
                            LocalDate date = dateTime.toLocalDate();
                            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                            this.getEventsInWeek(LocalDate.parse(formattedDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        });
                    }
                }
            }
        });
    }

    public void getEventsInWeek(LocalDate date) {
        CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate localBegin = DateHandler.getBeginningOfTheWeek(date);
                LocalDate localEnd = DateHandler.getEndOfTheWeek(date);
                String beginOffsetTime = localBegin.atStartOfDay().atZone(ZoneOffset.systemDefault())
                        .toOffsetDateTime().toString();
                String endOffsetTime = localEnd.atTime(LocalTime.MAX).atZone(ZoneOffset.systemDefault())
                        .toOffsetDateTime().toString();
                VoiceChatCalendar voiceChatCalendar = new VoiceChatCalendar();
                voiceChatCalendar.setStartWeekTime(beginOffsetTime);
                voiceChatCalendar.setEndWeekTime(endOffsetTime);
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
                            OffsetDateTime now = date.atStartOfDay().atZone(ZoneOffset.systemDefault())
                                    .toOffsetDateTime();
                            String formattedNow = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"));
                            calendarComponent.setCalendar(gridCalendarPane, events, formattedNow, this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void getEventsInCurrentWeek(CalendarController calendarController) {
        CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate localBegin = DateHandler.getBeginningOfTheWeek(LocalDate.now());
                LocalDate localEnd = DateHandler.getEndOfTheWeek(LocalDate.now());
                String beginOffsetTime = localBegin.atStartOfDay().atZone(ZoneOffset.systemDefault())
                        .toOffsetDateTime().toString();
                String endOffsetTime = localEnd.atTime(LocalTime.MAX).atZone(ZoneOffset.systemDefault())
                        .toOffsetDateTime().toString();
                VoiceChatCalendar voiceChatCalendar = new VoiceChatCalendar();
                voiceChatCalendar.setStartWeekTime(beginOffsetTime);
                voiceChatCalendar.setEndWeekTime(endOffsetTime);
                //voiceChatCalendar.setStartWeekTime("2025-12-22T00:00:00-08:00");
                //voiceChatCalendar.setEndWeekTime("2025-12-26T23:59:00-08:00");
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
                            OffsetDateTime now = OffsetDateTime.now();
                            String formattedNow = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"));
                            calendarComponent.setCalendar(gridCalendarPane, events, formattedNow, calendarController);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
