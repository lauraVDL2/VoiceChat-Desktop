package com.voicechat.client.call.component;

import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
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
import org.opencv.core.Mat;
import org.shared.pojo.ScreenShare;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ScreenShareComponent {

    private volatile boolean isSharing = false;
    private volatile boolean read = true;
    private Thread sendThread, captureThread;

    private final CallService callService = new CallService();

    public void setScreenShareButton(ImageView shareScreenButton, StackPane stackPane, String meetingId) {
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

            sendImage(screenChoiceList, stackPane, meetingId);

            HBox container = new HBox(screenChoiceList);
            container.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");
            popup.getContent().add(container);

            // Show popup near the button
            HBox hBox = (HBox) shareScreenButton.getParent();
            popup.show(hBox, event.getScreenX(), event.getScreenY() + 10);
        });
    }

    public void sendImage(ListView<Screen> screenChoiceList, StackPane stackPane, String meetingId) {
        screenChoiceList.setOnMouseClicked(e -> {
            Screen selectedScreen = screenChoiceList.getSelectionModel().getSelectedItem();
            if (selectedScreen != null) {
                System.out.println("Selected Screen: " + getScreenIndex(selectedScreen));
                isSharing = true;

                // Start a new thread for the continuous capture/send loop
                Thread sendLoopThread = new Thread(() -> {
                    try {
                        while (isSharing) {
                            Platform.runLater(() -> {
                                Robot robot = new Robot();
                                javafx.geometry.Rectangle2D bounds = selectedScreen.getBounds();
                                WritableImage screenCapture = new WritableImage((int) bounds.getWidth(), (int) bounds.getHeight());
                                robot.getScreenCapture(screenCapture, bounds);

                                // Prepare image and data
                                Image image = scaleImage(screenCapture, (int) bounds.getWidth(), (int) bounds.getHeight());
                                byte[] imageBytes = imageToByteArray(image);

                                // Create ScreenShare object
                                ScreenShare screenShare = new ScreenShare();
                                screenShare.setMeetingId(meetingId);
                                screenShare.setFrames(imageBytes);
                                screenShare.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());

                                // Send the frame
                                try {
                                    callService.screenShare(screenShare);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                            Thread.sleep(500);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                sendLoopThread.setDaemon(true);
                sendLoopThread.start();
            }
        });
    }

    public void receiveAndDisplay(CallController callController) {
        captureThread = new Thread(() -> {
            try {
                read = true;
                while (read) {
                    // Run UI update on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            //System.out.println("test ici");
                            callController.readScreen();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    // Sleep in background thread, not on UI thread
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void addScreenToNode(StackPane stackPane, byte[] bytes) {
        var node = stackPane.lookup("#screenShareImage");
        if (node == null) {
            ImageView imageView = new ImageView();
            imageView.setId("screenShareImage");
            imageView.setFitWidth(stackPane.getWidth());
            imageView.setFitHeight(stackPane.getHeight());
            imageView.setPreserveRatio(true);
            Image image = byteArrayToImage(bytes);
            imageView.setImage(image);
            stackPane.getChildren().add(imageView);
        }
        else {
            ImageView imageView = (ImageView) node;
            imageView.setFitWidth(stackPane.getWidth());
            imageView.setFitHeight(stackPane.getHeight());
            imageView.setPreserveRatio(true);
            Image image = byteArrayToImage(bytes);
            imageView.setImage(image);
        }
    }

    public byte[] imageToByteArray(Image frame) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(frame, null);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Image byteArrayToImage(byte[] imageBytes) {
        return new Image(new ByteArrayInputStream(imageBytes));
    }

    public void stopSend() {
        isSharing = false;
        if (sendThread != null) {
            try {
                sendThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopReceive() {
        read = false;
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
