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
import org.apache.commons.collections4.CollectionUtils;
import org.shared.mapped_entity.VoiceChatEvent;

import java.time.LocalDateTime;
import java.util.*;

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
            Set<VoiceChatEvent> overlapEventsAll = new HashSet<>();
            for (VoiceChatEvent event : events) {
                LocalDateTime localDateTimeStart = DateHandler.toLocalDateTime(event.getStart());
                LocalDateTime localDateTimeEnd = DateHandler.toLocalDateTime(event.getEnd());
                int col = localDateTimeStart.getDayOfWeek().getValue();
                int startEventHour = localDateTimeStart.getHour();
                int endEventHour = localDateTimeEnd.getHour();
                int differenceHour = endEventHour - startEventHour;
                int startEventMinute = localDateTimeStart.getMinute();
                int endEventMinute = localDateTimeEnd.getMinute();
                List<VoiceChatEvent> overlapEvents = overlapEvents(localDateTimeStart, localDateTimeEnd, events);
                if (CollectionUtils.isNotEmpty(overlapEventsAll)) {
                    if (overlapEventsAll.contains(event)) {
                        continue;
                    }
                }
                if (CollectionUtils.isNotEmpty(overlapEvents)) {
                    overlapEventsAll.addAll(overlapEvents);
                    int size = overlapEvents.size(); // Add the current event
                    double xAxis = 0.;
                    double fraction = (double) (1. / (double) size);
                    for (VoiceChatEvent overlapEvent : overlapEvents) {
                        if (differenceHour == 1) {
                            oneHourMeeting(fraction, xAxis, startEventMinute, endEventMinute, col, startEventHour, overlapEvent);
                        }
                        else if (differenceHour == 0) {
                            lessOneHourMeeting(fraction, xAxis, startEventMinute, endEventMinute, col, startEventHour, overlapEvent);
                        }
                        xAxis += fraction;
                    }
                } else {
                    if (differenceHour == 1) {
                        oneHourMeeting(1, 0, startEventMinute, endEventMinute, col, startEventHour, event);
                    }
                    // Less than 1 hour, fraction needed
                    else if (differenceHour == 0) {
                        lessOneHourMeeting(1, 0, startEventMinute, endEventMinute, col, startEventHour, event);
                    }
                    //fillCellFraction(col, 3, 0.5, 0.5, new Color(0.27, 0.51, 0.70, 1));
                }
                overlapEventsAll.add(event);
            }

            grid.setAlignment(Pos.CENTER);
            scrollPane.setContent(grid);
            rootPane.add(scrollPane, 1, 0);
        });
    }

    public void fillCellFraction(int col, int row, double xFraction, double xAxis, double yFraction, double yAxis, String eventText, String organizer, Color color) {
        Platform.runLater(() -> {
            Point2D coord = getCellCoordinate(col, row);
            Pane pane = cellMap.get(coord);
            if (pane != null) {
                // Retrieve overlay rectangles list or create if absent
                List<Rectangle> overlays = (List<Rectangle>) pane.getProperties().get("overlays");
                if (overlays == null) {
                    overlays = new ArrayList<>();
                    pane.getProperties().put("overlays", overlays);
                }

                // Create a new rectangle for this event
                Rectangle overlayRect = new Rectangle(CELL_WIDTH * xFraction, CELL_HEIGHT * yFraction);
                overlayRect.setFill(color);
                overlayRect.setArcWidth(10);
                overlayRect.setArcHeight(10);
                overlayRect.setStrokeWidth(0.5);
                overlayRect.setStroke(Color.DARKGRAY);

                // Position the rectangle within the cell
                double xPosition = CELL_WIDTH * xAxis;
                double yPosition = CELL_HEIGHT * yAxis;
                overlayRect.setLayoutX(xPosition);
                overlayRect.setLayoutY(yPosition);

                // Add the rectangle to the pane and overlays list
                pane.getChildren().add(overlayRect);
                overlays.add(overlayRect);

                // Optionally, add labels for event text
                if (eventText != null) {
                    VBox vBox = new VBox();
                    Label label = new Label(eventText);
                    Label organizerLabel = new Label(organizer);
                    vBox.getChildren().addAll(label, organizerLabel);
                    vBox.setLayoutX(xPosition + 2); // slight offset
                    vBox.setLayoutY(yPosition + 2);
                    vBox.setMaxWidth(CELL_WIDTH * xFraction - 4);
                    pane.getChildren().add(vBox);
                }
            }
        });
    }

    public Point2D getCellCoordinate(int col, int row) {
        // Calculate the top-left corner of the rectangle in pixels
        double x = (col) * CELL_WIDTH;
        double y = (row) * CELL_HEIGHT;
        return new Point2D(x, y);
    }

    public void lessOneHourMeeting(double xFraction, double xAxis, int startEventMinute, int endEventMinute, int col,
                                   int startEventHour, VoiceChatEvent event) {
        Platform.runLater(() -> {
            double yAxis = (double) ((double) startEventMinute / 60.);
            int differenceMinutes = endEventMinute - startEventMinute;
            double yFraction = (double) ((double) differenceMinutes / 60.);
            fillCellFraction(col, startEventHour + 1, xFraction, xAxis, yFraction, yAxis, event.getSubject(), event.getOrganizer(),
                    new Color(0.27, 0.51, 0.70, 1));
        });
    }

    public void oneHourMeeting(double xFraction, double xAxis, int startEventMinute, int endEventMinute, int col,
                               int startEventHour, VoiceChatEvent event) {
        Platform.runLater(() -> {
            double yAxis = (double) ((double) startEventMinute / 60.);
            if (startEventMinute == 0 && endEventMinute == 0) {
                fillCellFraction(col, startEventHour + 1, xFraction, xAxis, 1, yAxis, event.getSubject(), event.getOrganizer(),
                        new Color(0.27, 0.51, 0.70, 1));
            } else if (endEventMinute > 0) {
                double endFraction = (double) ((double) endEventMinute / 60.);
                fillCellFraction(col, startEventHour + 1, xFraction, xAxis,1 - yAxis, yAxis, event.getSubject(), event.getOrganizer(),
                        new Color(0.27, 0.51, 0.70, 1));
                fillCellFraction(col, startEventHour + 2, xFraction, xAxis, endFraction, 0, null, null,
                        new Color(0.27, 0.51, 0.70, 1));
            }
        });
    }

    public List<VoiceChatEvent> overlapEvents(LocalDateTime startA, LocalDateTime endA, List<VoiceChatEvent> events) {
        return events.stream().filter(event -> {
            LocalDateTime startB = DateHandler.toLocalDateTime(event.getStart());
            LocalDateTime endB = DateHandler.toLocalDateTime(event.getEnd());
            if (!startA.isAfter(endB) && !startB.isAfter(endA)) {
                return true;
            }
            return false;
        }).toList();
    }
}