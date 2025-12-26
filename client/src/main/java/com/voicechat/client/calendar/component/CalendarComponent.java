package com.voicechat.client.calendar.component;

import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.shared.mapped_entity.VoiceChatEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarComponent {
    private static final int CELL_WIDTH = 240;
    private static final int CELL_HEIGHT = 120;

    private Map<Point2D, Pane> cellMap = new HashMap<>();

    public void setCalendar(GridPane rootPane, List<VoiceChatEvent> events) {
        Platform.runLater(() -> {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            GridPane grid = new GridPane();

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            int startHour = 0;
            int endHour = 24;

            // Add day labels
            for (int col = 0; col < days.length; col++) {
                // Add day headers
                Label dayLabel = new Label(days[col]);
                dayLabel.setAlignment(Pos.CENTER);
                grid.add(dayLabel, col + 1, 0);
            }

            // Add time labels and cells
            for (int row = 1; row <= endHour - startHour; row++) {
                // Time labels
                Label timeLabel = new Label((startHour + row - 1) + ":00");
                grid.add(timeLabel, 0, row);

                for (int col = 0; col < days.length; col++) {
                    // Background rectangle
                    Rectangle bgRect = new Rectangle(CELL_WIDTH, CELL_HEIGHT);
                    bgRect.setFill(Color.LIGHTGRAY);
                    bgRect.setStroke(Color.DARKGRAY);
                    bgRect.setArcWidth(10);
                    bgRect.setArcHeight(10);
                    bgRect.setStrokeWidth(0.5);

                    Pane pane = new Pane();
                    // Overlay rectangle
                    Rectangle overlayRect = new Rectangle(CELL_WIDTH, 0); // height will be set dynamically
                    overlayRect.setFill(Color.ORANGE);
                    overlayRect.setStroke(null);
                    overlayRect.setVisible(false);
                    pane.getChildren().add(overlayRect);

                    // Use a Pane to position overlay at a specific Y
                    Pane cellPane = new Pane();
                    cellPane.getChildren().addAll(bgRect, pane);

                    grid.add(cellPane, col + 1, row);

                    // Store overlayRect for later
                    Point2D coord = getCellCoordinate(col + 1, row);
                    cellMap.put(coord, pane);
                }
            }
            for (VoiceChatEvent event : events) {
                System.out.println("loop events");
                LocalDateTime localDateTimeStart = DateHandler.toLocalDateTime(event.getStart());
                LocalDateTime localDateTimeEnd = DateHandler.toLocalDateTime(event.getEnd());
                int col = localDateTimeStart.getDayOfWeek().getValue();
                int startEventHour = localDateTimeStart.getHour();
                int endEventHour = localDateTimeEnd.getHour();
                int differenceHour = endEventHour - startEventHour;
                int startEventMinute = localDateTimeStart.getMinute();
                int endEventMinute = localDateTimeEnd.getMinute();
                // 1 hour meeting
                if (differenceHour == 1) {
                    System.out.println("va diff 1");
                    double yAxis = (double) ((double)startEventMinute / 60.);
                    if (startEventMinute == 0 && endEventMinute == 0) {
                        fillCellFraction(col, startEventHour + 1, 1, yAxis, event.getSubject(), event.getOrganizer(),
                                new Color(0.27, 0.51, 0.70, 1));
                    }
                    else if (endEventMinute > 0) {
                        double endFraction = (double) ((double)endEventMinute / 60.);
                        fillCellFraction(col, startEventHour + 1, 1 - yAxis, yAxis, event.getSubject(), event.getOrganizer(),
                                new Color(0.27, 0.51, 0.70, 1));
                        fillCellFraction(col, startEventHour + 2, endFraction, 0, null, null,
                                new Color(0.27, 0.51, 0.70, 1));
                    }
                }
                // Less than 1 hour, fraction needed
                else if (differenceHour == 0) {
                    System.out.println("va diff 0");
                    double yAxis = (double) ((double)startEventMinute / 60.);
                    int differenceMinutes = endEventMinute - startEventMinute;
                    double fraction = (double) ((double)differenceMinutes / 60.);
                    fillCellFraction(col, startEventHour + 1, fraction, yAxis, event.getSubject(), event.getOrganizer(),
                            new Color(0.27, 0.51, 0.70, 1));
                }
                //fillCellFraction(col, 3, 0.5, 0.5, new Color(0.27, 0.51, 0.70, 1));
            }

            grid.setAlignment(Pos.CENTER);
            scrollPane.setContent(grid);
            rootPane.add(scrollPane, 1, 0);
        });
    }

    public void fillCellFraction(int col, int row, double fraction, double yAxis, String eventText, String organizer, Color color) {
        Point2D coord = getCellCoordinate(col, row);
        javafx.scene.layout.Pane pane = cellMap.get(coord);
        Rectangle overlayRect =  (Rectangle) pane.getChildren().getFirst();
        if (overlayRect != null) {
            overlayRect.setWidth(CELL_WIDTH);
            overlayRect.setFill(color);
            overlayRect.setArcWidth(10);
            overlayRect.setArcHeight(10);
            overlayRect.setStrokeWidth(0.5);
            overlayRect.setHeight(CELL_HEIGHT * fraction);
            overlayRect.setVisible(true);

            // Position the overlay rectangle vertically based on fraction
            double yPosition = CELL_HEIGHT * yAxis;
            overlayRect.setLayoutY(yPosition);
        }
        if (organizer != null) {
            VBox vBox = new VBox();
            Label label = new Label();
            label.setText(eventText);
            vBox.setLayoutX(10);
            vBox.setLayoutY(yAxis * CELL_HEIGHT);
            Label organizerLabel = new Label(organizer);
            vBox.getChildren().addAll(label, organizerLabel);
            pane.getChildren().add(vBox);
        }
    }

    public Point2D getCellCoordinate(int col, int row) {
        // Calculate the top-left corner of the rectangle in pixels
        double x = (col) * CELL_WIDTH;
        double y = (row) * CELL_HEIGHT;
        return new Point2D(x, y);
    }
}