package com.voicechat.client.call.component;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class CameraSelectorComponent extends AbstractCamera {

    public void startCamera(ImageView imageView) {
        capture = new VideoCapture();
        capture.open(0); // Open default camera device

        if (capture.isOpened()) {
            cameraActive = true;

            // Thread to read frames
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while (cameraActive) {
                        Mat frame = new Mat();
                        if (capture.read(frame)) {
                            Image imageToShow = mat2Image(frame);
                            Platform.runLater(() -> imageView.setImage(imageToShow));
                        }
                    }
                    capture.release();
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        } else {
            System.out.println("Failed to open camera");
        }
    }

    public void stop() {
        cameraActive = false;
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }

    public boolean isCameraActive() {
        return cameraActive;
    }
}
