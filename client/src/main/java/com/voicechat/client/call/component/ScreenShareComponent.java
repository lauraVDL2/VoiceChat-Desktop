package com.voicechat.client.call.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.scene.robot.Robot;

import java.awt.image.BufferedImage;
import java.util.List;

public class ScreenShareComponent {

    public void setScreenShareButton(ImageView shareScreenButton, StackPane stackPane) {
        shareScreenButton.setOnMouseClicked((event) -> {
            List<Screen> screens = Screen.getScreens();
            ObservableList<Screen> screenList = FXCollections.observableArrayList(screens);

            Popup popup = new Popup();

            ListView<Screen> screenChoiceList = new ListView<>(screenList);
            screenChoiceList.setCellFactory(listView -> new ListCell<Screen>() {
                private final ImageView imageView = new ImageView();

                @Override
                protected void updateItem(Screen screen, boolean empty) {
                    super.updateItem(screen, empty);
                    if (empty || screen == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Image thumbnail = getScreenThumbnail(screen);
                        imageView.setImage(thumbnail);
                        imageView.setFitWidth(100);
                        imageView.setFitHeight(50);
                        imageView.setPreserveRatio(true);

                        Label label = new Label("Screen " + getScreenIndex(screen));
                        VBox vBox = new VBox(10, imageView, label);
                        setGraphic(vBox);
                    }
                }
            });

            // Optional: handle selection
            screenChoiceList.setOnMouseClicked(e -> {
                Screen selectedScreen = screenChoiceList.getSelectionModel().getSelectedItem();
                if (selectedScreen != null) {
                    // Handle the selected screen here
                    System.out.println("Selected Screen: " + getScreenIndex(selectedScreen));
                    try {
                        Robot robot = new Robot();
                        javafx.geometry.Rectangle2D bounds = selectedScreen.getBounds();
                        Rectangle captureRect = new Rectangle(
                                (int) bounds.getMinX(),
                                (int) bounds.getMinY(),
                                (int) bounds.getWidth(),
                                (int) bounds.getHeight()
                        );
                        WritableImage screenCapture = new WritableImage((int) bounds.getWidth(), (int) bounds.getHeight());
                        robot.getScreenCapture(screenCapture, bounds);
                        ImageView imageView = new ImageView();
                        imageView.setFitWidth(stackPane.getWidth());
                        imageView.setFitHeight(stackPane.getHeight());
                        imageView.setPreserveRatio(true);
                        Image image = scaleImage(screenCapture, (int) bounds.getWidth(), (int) bounds.getHeight());
                        imageView.setImage(image);
                        stackPane.getChildren().add(imageView);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            HBox container = new HBox(screenChoiceList);
            container.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");
            popup.getContent().add(container);

            // Show popup near the button
            HBox hBox = (HBox) shareScreenButton.getParent();
            popup.show(hBox, event.getScreenX(), event.getScreenY() + 10);
        });
    }

    private Image getScreenThumbnail(Screen screen) {
        int thumbnailWidth = 100;
        int thumbnailHeight = 50;

        Rectangle2D bounds = screen.getBounds();

        WritableImage screenCapture = new WritableImage((int) bounds.getWidth(), (int) bounds.getHeight());
        Robot robot = new Robot();
        robot.getScreenCapture(screenCapture, bounds);

        return scaleImage(screenCapture, thumbnailWidth, thumbnailHeight);
    }

    private Image scaleImage(Image srcImage, int targetWidth, int targetHeight) {
        SnapshotParameters params = new SnapshotParameters();
        Canvas canvas = new Canvas(targetWidth, targetHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.drawImage(srcImage, 0, 0, targetWidth, targetHeight);
        return canvas.snapshot(params, new WritableImage(targetWidth, targetHeight));
    }

    private int getScreenIndex(Screen screen) {
        List<Screen> screens = Screen.getScreens();
        return screens.indexOf(screen) + 1; // 1-based index
    }
}
