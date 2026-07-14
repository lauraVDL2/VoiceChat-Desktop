package com.voicechat.client.call.component;

import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.shared.pojo.ScreenShare;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScreenShareComponent {

    private volatile boolean isSharing = false;
    private volatile boolean read = true;
    private Thread sendThread, captureThread;

    private final CallService callService = new CallService();
    private AnimationTimer serviceSend, serviceReceive;
    private WritableImage previousFrame = null;
    private Robot robot;

    public void setupScreenSharing() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

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
        setupScreenSharing();
        screenChoiceList.setOnMouseClicked(e -> {
            Screen selectedScreen = screenChoiceList.getSelectionModel().getSelectedItem();
            if (selectedScreen != null) {
                isSharing = true;

                // Cancel existing timer if running
                if (serviceSend != null) {
                    serviceSend.stop();
                }

                // Create and start new AnimationTimer
                serviceSend = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        try {
                            Rectangle2D screenBounds = selectedScreen.getBounds();
                            WritableImage screenCapture = new WritableImage(
                                    (int) screenBounds.getWidth(),
                                    (int) screenBounds.getHeight());

                            robot.getScreenCapture(screenCapture, screenBounds);

                            Image image = scaleImage(screenCapture, (int) screenBounds.getWidth(), (int) screenBounds.getHeight());

                            byte[] imageBytes = imageToByteArray(image);

                            // Create ScreenShare object
                            ScreenShare screenShare = new ScreenShare();
                            screenShare.setMeetingId(meetingId);
                            screenShare.setFrames(imageBytes);
                            screenShare.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());

                            // Send the frame
                            callService.screenShare(screenShare);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                serviceSend.start();
            }
        });
    }

    public void receiveAndDisplay(CallController callController) {
        // Stop existing timer if running
        if (serviceReceive != null) {
            serviceReceive.stop();
        }

        // Create new AnimationTimer for receiving/displaying
        serviceReceive = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    callController.readScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        serviceReceive.start();
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

    public byte[] imageToByteArray(Image frame) throws IOException {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(frame, null);


        if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage newBufferedImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, null);
            bufferedImage = newBufferedImage;
        }

        byte[] result = new byte[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Get a JPEG ImageWriter
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new RuntimeException("No JPG ImageWriter found");
        }
        ImageWriter writer = writers.next();
        // Set up output
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        // Configure compression quality
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f); // Set compression quality here
        }
        // Write image
        writer.write(null, new IIOImage(bufferedImage, null, null), param);
        result = baos.toByteArray();
        System.out.println(result.length);
        writer.dispose();
        baos.close();
        return result;
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
