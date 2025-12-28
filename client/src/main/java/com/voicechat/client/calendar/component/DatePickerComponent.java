package com.voicechat.client.calendar.component;

import com.voicechat.client.calendar.controller.CalendarController;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatePickerComponent {
    private static final String PATTERN = "dd/MM/yyyy";

    public void setDatePicker(VBox vBox, CalendarController calendarController) {
        DatePicker datePicker = new DatePicker();
        // Set prompt text
        datePicker.setPromptText("Select a date");

        // Define formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN);

        // Set converter for custom display format
        datePicker.setConverter(new StringConverter<LocalDate>() {
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
        });

        // Add listener for date changes
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                calendarController.getEventsInWeek(newDate);
            }
        });

        // Layout
        VBox root = new VBox(10, datePicker);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        vBox.getChildren().add(root);
    }
}
