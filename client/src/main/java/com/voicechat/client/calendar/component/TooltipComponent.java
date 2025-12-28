package com.voicechat.client.calendar.component;

import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;

import java.time.LocalDate;

public class TooltipComponent {

    private Rectangle previousRectangle = null;
    private Popup previousTooltip = null;

    public void initCreateMeetingTooltip(Rectangle rectangle, String currentDay, int row) {
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

                Region margin = new Region();
                margin.setPrefHeight(15);

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
                startDatePicker.setValue(LocalDate.parse(currentDay));
                startDatePicker.getStyleClass().add("calendarComboBox");
                DatePicker endDatePicker = new DatePicker();
                endDatePicker.setValue(LocalDate.parse(currentDay));
                endDatePicker.getStyleClass().add("calendarComboBox");
                HBox startHbox = new HBox();
                startHbox.getChildren().addAll(startDatePicker, startHour);
                HBox endHbox = new HBox();
                endHbox.getChildren().addAll(endDatePicker, endHour);

                vBox.getChildren().addAll(meetingName, margin, startHbox, endHbox);
                tooltip.getContent().add(vBox);
                rectangle.setFill(new Color(0.27, 0.51, 0.70, 1));
                tooltip.show(rectangle, event.getScreenX(), event.getScreenY() + 10);
                previousRectangle = rectangle;
                previousTooltip = tooltip;
            });
        });
    }

}
