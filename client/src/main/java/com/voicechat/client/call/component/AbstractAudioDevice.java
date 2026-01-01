package com.voicechat.client.call.component;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractAudioDevice {

    public double calculateRMS(byte[] buffer, int bytesRead) {
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead);
        bb.order(ByteOrder.BIG_ENDIAN); // or LITTLE_ENDIAN depending on your format
        long sum = 0;
        int sampleCount = bytesRead / 2;

        for (int i = 0; i < sampleCount; i++) {
            short sample = bb.getShort(i * 2);
            sum += sample * sample;
        }

        double mean = sum / (double) sampleCount;
        return Math.sqrt(mean);
    }

    // Convert RMS to a 0-1 range for ProgressBar
    public double rmsToProgress(double rms) {
        // Adjust the denominator based on your environment
        double maxRMS = 32768; // Max for 16-bit audio
        double normalized = rms / maxRMS;
        // Optional: add smoothing or thresholding
        return Math.min(normalized, 1.0);
    }

    public TargetDataLine getDefaultMicrophone() throws LineUnavailableException {
        // Define an audio format (sample rate, sample size, channels, etc.)
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        // Create info object for TargetDataLine
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        // Get the line (default microphone)
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        return line;
    }

    public TargetDataLine getTargetDataLineForPort(Port.Info portInfo) throws LineUnavailableException {
        // Find a mixer that supports the port
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            // Check if this mixer supports the port info
            Line.Info[] lineInfos = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                if (lineInfo instanceof Port.Info) {
                    if (lineInfo.equals(portInfo)) {
                        // Found the mixer supporting that port
                        // Now, get the TargetDataLine from this mixer
                        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, null);
                        if (mixer.isLineSupported(dataLineInfo)) {
                            TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
                            line.open();
                            return line;
                        }
                    }
                }
            }
        }
        return null;
    }
}
