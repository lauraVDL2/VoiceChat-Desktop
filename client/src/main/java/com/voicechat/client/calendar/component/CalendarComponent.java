package com.voicechat.client.calendar.component;

import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import org.apache.commons.collections4.CollectionUtils;
import org.shared.mapped_entity.VoiceChatEvent;

import java.time.LocalDateTime;
import java.util.*;

public class CalendarComponent {
    private static final double CELL_WIDTH = Screen.getPrimary().getVisualBounds().getWidth() / 7.;
    private static final int CELL_WIDTH_PADDING = 20;
    private static final int CELL_HEIGHT = 120;

    private Map<Point2D, Pane> cellMap = new HashMap<>();

    private final DatePickerComponent datePickerComponent = new DatePickerComponent();

    public void setCalendar(GridPane rootPane, List<VoiceChatEvent> events, String currentDate) {
        ScrollPane scrollPaneGrid = new ScrollPane();
        Platform.runLater(() -> {
            GridPane grid = new GridPane();

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            int startHour = 0;
            int endHour = 24;

            var daysOfWeek = DateHandler.getDaysOfWeek(DateHandler.toLocalDateTime(currentDate), days.length);

            scrollPaneGrid.setFitToWidth(true);
            scrollPaneGrid.setPrefWidth(Screen.getPrimary().getVisualBounds().getWidth());
            scrollPaneGrid.getStyleClass().add("scrollPaneCalendar");
            GridPane gridCalendar = new GridPane();
            gridCalendar.setAlignment(Pos.CENTER);
            GridPane gridDays = new GridPane();
            gridDays.setAlignment(Pos.CENTER);

            // Add day labels
            for (int col = 0; col < days.length; col++) {
                // Add day headers
                VBox box = new VBox();
                box.setPrefWidth(CELL_WIDTH + CELL_WIDTH_PADDING);
                Label dayLabel = new Label(days[col] + " " + daysOfWeek.get(col).getDayOfMonth());
                dayLabel.getStyleClass().add("dayLabel");
                box.setAlignment(Pos.CENTER);
                box.getChildren().add(dayLabel);
                gridDays.add(box, col + 1, 0);
            }

            // Add time labels and cells
            for (int row = 1; row <= endHour - startHour; row++) {
                // Time labels

                Label timeLabel = new Label((startHour + row - 1) + ":00");
                gridCalendar.add(timeLabel, 0, row);

                for (int col = 0; col < days.length; col++) {
                    // Background rectangle
                    Rectangle bgRect = new Rectangle(CELL_WIDTH + CELL_WIDTH_PADDING, CELL_HEIGHT);
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

                    gridCalendar.add(cellPane, col + 1, row);

                    // Store overlayRect for later
                    Point2D coord = getCellCoordinate(col + 1, row);
                    cellMap.put(coord, pane);
                }
            }
            Set<VoiceChatEvent> overlapEventsAll = new HashSet<>();
            for (VoiceChatEvent event : events) {
                LocalDateTime localDateTimeStart = DateHandler.toLocalDateTime(event.getStart());
                LocalDateTime localDateTimeEnd = DateHandler.toLocalDateTime(event.getEnd());
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
                        LocalDateTime eventStart = DateHandler.toLocalDateTime(overlapEvent.getStart());
                        LocalDateTime eventEnd = DateHandler.toLocalDateTime(overlapEvent.getEnd());
                        int ovCol = eventStart.getDayOfWeek().getValue();
                        int ovStartEventHour = eventStart.getHour() + 1;
                        int ovEndEventHour = eventEnd.getHour() + 1;
                        int ovDifferenceHour = ovEndEventHour - ovStartEventHour;
                        int ovStartEventMinute = eventStart.getMinute();
                        int ovEndEventMinute = eventEnd.getMinute();
                        if (ovDifferenceHour == 1) {
                            oneHourMeeting(fraction, xAxis, ovStartEventMinute, ovEndEventMinute, ovCol,
                                    ovStartEventHour, overlapEvent);
                        }
                        else if (ovDifferenceHour == 0) {
                            lessOneHourMeeting(fraction, xAxis, ovStartEventMinute, ovEndEventMinute, ovCol,
                                    ovStartEventHour, overlapEvent);
                        }
                        else if (ovDifferenceHour > 1) {
                            moreOnHourMeeting(fraction, xAxis, ovStartEventMinute, ovEndEventMinute, ovCol,
                                    ovStartEventHour, ovEndEventHour, overlapEvent);
                        }
                        xAxis += fraction;
                    }
                }
                overlapEventsAll.add(event);
            }

            grid.setAlignment(Pos.CENTER);
            VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            datePickerComponent.setDatePicker(vBox);
            scrollPaneGrid.setContent(gridCalendar);
            grid.add(scrollPaneGrid, 0, 0);
            vBox.getChildren().addAll(gridDays, grid);
            rootPane.add(vBox, 1, 0);
        });

        // Example: scroll to 14:00 (2 PM)
        int targetHour = LocalDateTime.now().getHour();

        Platform.runLater(() -> {
            // Optional: small delay to ensure layout is ready
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Call the method to scroll to the desired hour
            scrollToHour(scrollPaneGrid, targetHour);
        });
    }

    public void fillCellFraction(int col, int row, double xFraction, double xAxis, double yFraction, double yAxis,
                                 String eventText, String organizer, Color color) {
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
        double yAxis = (double) ((double) startEventMinute / 60.);
        int differenceMinutes = endEventMinute - startEventMinute;
        double yFraction = (double) ((double) differenceMinutes / 60.);
        fillCellFraction(col, startEventHour + 1, xFraction, xAxis, yFraction, yAxis, event.getSubject(), event.getOrganizer(),
                new Color(0.27, 0.51, 0.70, 1));
    }

    public void oneHourMeeting(double xFraction, double xAxis, int startEventMinute, int endEventMinute, int col,
                               int startEventHour, VoiceChatEvent event) {
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
    }

    public void moreOnHourMeeting(double xFraction, double xAxis, int startEventMinute, int endEventMinute, int col,
                                  int startEventHour, int endEventHour, VoiceChatEvent event) {
        double yAxis = (double) ((double) startEventMinute / 60.);
        fillCellFraction(col, startEventHour + 1, xFraction, xAxis, 1 - yAxis, yAxis, event.getSubject(), event.getOrganizer(),
                new Color(0.27, 0.51, 0.70, 1));
        for (int i = startEventHour + 2; i < endEventHour + 1; i++) {
            fillCellFraction(col, i, xFraction, xAxis, 1, 0, null, null,
                    new Color(0.27, 0.51, 0.70, 1));
        }
        if (endEventMinute > 0) {
            double endFraction = (double) ((double) endEventMinute / 60.);
            fillCellFraction(col, endEventHour + 1, xFraction, xAxis, endFraction, 0, null, null,
                    new Color(0.27, 0.51, 0.70, 1));
        }
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

    public void scrollToPane(ScrollPane scrollPane, Pane targetPane) {
        Platform.runLater(() -> {
            // Ensure layout is updated
            scrollPane.getContent().applyCss();
            scrollPane.getContent().layoutYProperty();

            // Convert target pane's bounds to scene coordinates
            Bounds boundsInScene = targetPane.localToScene(targetPane.getBoundsInLocal());
            // Convert scene coordinates to content coordinates
            Bounds contentBounds = scrollPane.getContent().localToScene(scrollPane.getContent().getBoundsInLocal());

            double yInContent = boundsInScene.getMinY() - contentBounds.getMinY();

            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            double vvalue = yInContent / (contentHeight - viewportHeight);
            vvalue = Math.max(0, Math.min(vvalue, 1));

            scrollPane.setVvalue(vvalue);
        });
    }

    public void scrollToHour(ScrollPane scrollPane, int targetHour) {
        int row = targetHour + 1; // since rows start at 1 for 0:00
        Point2D coord = getCellCoordinate(1, row);
        Pane targetPane = cellMap.get(coord);
        if (targetPane != null) {
            scrollToPane(scrollPane, targetPane);
        }
    }

}