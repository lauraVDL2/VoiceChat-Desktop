package com.voicechat.client.call.component;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;

public class CameraComponent {

    static { OpenCV.loadShared(); }

    private VideoCapture capture;
    private boolean cameraActive = false;

    public void startCamera(ImageView imageView) {
        capture = new VideoCapture();
        capture.open(0); // Open default camera device

        if (capture.isOpened()) {
            cameraActive = true;

            // Thread to read frames
            new Thread(() -> {
                while (cameraActive) {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {
                        Image imageToShow = mat2Image(frame);
                        Platform.runLater(() -> imageView.setImage(imageToShow));
                    }
                }
                capture.release();
            }).start();
        } else {
            System.out.println("Failed to open camera");
        }
    }

    private Image mat2Image(Mat frame) {
        // Convert Mat to BufferedImage
        BufferedImage bufferedImage = null;
        try {
            // Convert the Mat object (OpenCV) to BufferedImage (AWT)
            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (frame.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }
            byte[] b = new byte[(int) (frame.total() * frame.channels())];
            frame.get(0, 0, b);
            bufferedImage = new BufferedImage(frame.cols(), frame.rows(), type);
            bufferedImage.getRaster().setDataElements(0, 0, frame.cols(), frame.rows(), b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Convert BufferedImage to JavaFX Image
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    public void stop() {
        cameraActive = false;
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }
}
