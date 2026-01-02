package com.voicechat.client.call.component;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class AbstractCamera {
    static { OpenCV.loadShared(); }

    protected VideoCapture capture;
    protected boolean cameraActive = false;

    public Image mat2Image(Mat frame) {
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

    // Convert Mat to BufferedImage
    public BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image;
        if (channels == 3) {
            // BGR to RGB
            for (int i = 0; i < sourcePixels.length; i += 3) {
                byte temp = sourcePixels[i];
                sourcePixels[i] = sourcePixels[i + 2];
                sourcePixels[i + 2] = temp;
            }
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else if (channels == 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        } else {
            // handle other cases if needed
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }

        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
        return image;
    }

}
