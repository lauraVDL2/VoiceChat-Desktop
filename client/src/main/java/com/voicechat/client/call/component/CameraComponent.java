package com.voicechat.client.call.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.shared.pojo.Camera;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraComponent extends AbstractCamera {

    private Thread captureThread, sendThread;

    private volatile boolean receive;

    private final CallService callService = new CallService();

    public void sendFrames(String meetingId, boolean isCameraActive) {
        capture = new VideoCapture();
        capture.open(0); // Open default camera device

        if (capture.isOpened()) {
            cameraActive = isCameraActive;

            // Thread to read frames
            sendThread = new Thread(() -> {
                while (cameraActive) {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {
                        Image imageToShow = mat2Image(frame);
                        byte[] frames = matToByteArray(frame);
                        Camera camera = new Camera();
                        camera.setMeetingId(meetingId);
                        camera.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());
                        camera.setFrames(frames);
                        try {
                            callService.sendCamera(camera);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                capture.release();
            });
            sendThread.setDaemon(true);
            sendThread.start();
        } else {
            System.out.println("Failed to open camera");
        }
    }

    public byte[] matToByteArray(Mat frame) {
        BufferedImage bufferedImage = matToBufferedImage(frame);
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

    public void receiveAndDisplay(ImageView imageView, String meetingId, CallController callController) {
        captureThread = new Thread(() -> {
            try {
                receive = true;
                while (receive) {
                    callController.readCamera(meetingId, imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopSend() {
        cameraActive = false;
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        if (sendThread != null) {
            try {
                sendThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopReceive() {
        receive = false;
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
