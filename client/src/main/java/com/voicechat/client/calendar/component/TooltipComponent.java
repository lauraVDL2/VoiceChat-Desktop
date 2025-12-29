package com.voicechat.client.calendar.component;

import com.voicechat.client.calendar.controller.CalendarController;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.StringConverter;
import org.controlsfx.control.ToggleSwitch;
import org.shared.entity.User;
import org.shared.mapped_entity.VoiceChatEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TooltipComponent {

    private Rectangle previousRectangle = null;
    private Popup previousTooltip = null;

    private static final String PATTERN = "yyyy-MM-dd";

    public void initCreateMeetingTooltip(Rectangle rectangle, String currentDay, int row, CalendarController calendarController) {
        Platform.runLater(() -> {
            Popup tooltip = new Popup();

            rectangle.setOnMouseClicked(event -> {
                if (previousRectangle != null && previousTooltip != null) {
                    previousRectangle.setFill(Color.LIGHTGRAY);
                    previousTooltip.hide();
                }
                VBox vBox = new VBox();
                vBox.setAlignment(Pos.CENTER);
                vBox.getStyleClass().add("calendarTooltip");
                TextField meetingName = new TextField();
                meetingName.setPromptText("Enter a title");
                meetingName.getStyleClass().add("calendarMainTextfield");
                ComboBox<String> startHour = new ComboBox<>();
                ComboBox<String> endHour = new ComboBox<>();
                startHour.getStyleClass().add("calendarComboBox");
                endHour.getStyleClass().add("calendarComboBox");

                Region margin1 = new Region();
                margin1.setPrefHeight(15);

                for (int hour = 0; hour < 24; hour++) {
                    for (int minute = 0; minute < 60; minute += 15) {
                        startHour.getItems().add(DateHandler.formattedTime(hour) + ":" + DateHandler.formattedTime(minute));
                        endHour.getItems().add(DateHandler.formattedTime(hour) + ":" + DateHandler.formattedTime(minute));
                    }
                    if (hour == row) {
                        startHour.setValue(startHour.getItems().get(hour * 4 - 4));
                        endHour.setValue(endHour.getItems().get(hour * 4));
                    }
                }
                DatePicker startDatePicker = new DatePicker();
                startDatePicker.setConverter(initConverter());
                startDatePicker.setValue(LocalDate.parse(currentDay));
                startDatePicker.getStyleClass().add("calendarComboBox");

                DatePicker endDatePicker = new DatePicker();
                endDatePicker.setConverter(initConverter());
                endDatePicker.setValue(LocalDate.parse(currentDay));
                endDatePicker.getStyleClass().add("calendarComboBox");

                HBox startHbox = new HBox();
                startHbox.getChildren().addAll(startDatePicker, startHour);
                HBox endHbox = new HBox();
                endHbox.getChildren().addAll(endDatePicker, endHour);

                Region marginBottom2 = new Region();
                marginBottom2.setPrefHeight(15);

                HBox toggleHbox = new HBox();
                toggleHbox.setAlignment(Pos.CENTER);
                Label onlineMeetingLabel = new Label("Is online meeting ?");
                ToggleSwitch toggleSwitch = new ToggleSwitch();
                toggleSwitch.setSelected(false);
                toggleHbox.getChildren().addAll(onlineMeetingLabel, toggleSwitch);

                Region marginBottom3 = new Region();
                marginBottom3.setPrefHeight(15);

                Button button = new Button();
                button.setText("Create");
                button.getStyleClass().add("eventButton");

                vBox.getChildren().addAll(meetingName, margin1, startHbox, endHbox, marginBottom2,
                        toggleHbox, marginBottom3, button);
                tooltip.getContent().add(vBox);
                rectangle.setFill(new Color(0.27, 0.51, 0.70, 1));
                tooltip.show(rectangle, event.getScreenX(), event.getScreenY() + 10);
                eventCreateRequestMapping(button, toggleSwitch, startDatePicker, endDatePicker, startHour, endHour, meetingName,
                        calendarController);
                previousRectangle = rectangle;
                previousTooltip = tooltip;
            });
        });
    }

    public void eventCreateRequestMapping(Button button, ToggleSwitch toggleSwitch, DatePicker startDate, DatePicker endDate,
                                                    ComboBox<String> startHour, ComboBox<String> endHour, TextField meetingName,
                                          CalendarController calendarController) {
        button.setOnAction(e -> {
            VoiceChatEvent voiceChatEvent = new VoiceChatEvent();
            voiceChatEvent.setOnlineMeeting(toggleSwitch.isSelected());
            voiceChatEvent.setTimezone(ZoneId.systemDefault().toString());
            String start = startDate.getValue() + "T" + startHour.getValue() + ":00";
            String end = endDate.getValue() + "T" + endHour.getValue() + ":00";
            voiceChatEvent.setStart(start);
            voiceChatEvent.setEnd(end);
            voiceChatEvent.setSubject(meetingName.getText());
            voiceChatEvent.setOrganizer(UserSession.INSTANCE.getUser().getEmailAddress());
            calendarController.createEvent(voiceChatEvent);
        });
    }

    public StringConverter<LocalDate> initConverter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN);
        return new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return formatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, formatter);
                } else {
                    return null;
                }
            }
        };
    }

}
