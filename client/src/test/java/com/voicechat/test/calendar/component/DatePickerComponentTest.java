package com.voicechat.test.calendar.component;

import com.voicechat.client.calendar.component.DatePickerComponent;
import com.voicechat.client.calendar.controller.CalendarController;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
public class DatePickerComponentTest extends FxRobot {
    private DatePickerComponent datePickerComponent;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        datePickerComponent = new DatePickerComponent();
    }

    @Test
    void testSetDatePicker() {
        VBox vBox = new VBox();
        CalendarController calendarController = mock(CalendarController.class);

        datePickerComponent.setDatePicker(vBox, calendarController);

        assertNotNull(vBox);
        assertFalse(vBox.getChildren().isEmpty());
    }
}
