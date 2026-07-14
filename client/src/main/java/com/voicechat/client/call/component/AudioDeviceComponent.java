package com.voicechat.client.call.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import de.maxhenkel.opus4j.OpusDecoder;
import de.maxhenkel.opus4j.OpusEncoder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.apache.commons.collections4.CollectionUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.shared.pojo.Voice;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class AudioDeviceComponent extends AbstractAudioDevice {
    private boolean microphoneCut = true;

    private volatile boolean capturing = false;
    private volatile boolean listening = false;
    private Thread captureThread;
    private short[] leftoverSamples = new short[0];
    private CompletableFuture<Void> listeningFuture;
    private double rms = 0;
    private AudioFormat supportedFormat;
    //private final ReentrantLock lock = new ReentrantLock();

    private final CallService callService = new CallService();

    public void setAudioDevice(boolean microphoneCut, Mixer.Info selectedHeadMixerInfo, Mixer.Info selectedMicMixerInfo,
                               AudioFormat audioFormat, String meetingId, CallController callController) {
        this.microphoneCut = microphoneCut;
        initializeOpus();
        startVolumeMonitoring(selectedHeadMixerInfo, callController);
        startAudioCapture(selectedMicMixerInfo, meetingId);
    }

    public void receiveAndPlay(Mixer.Info selectedMixerInfo, CallController callController) throws LineUnavailableException {
        listening = true;
        byte[] buffer = new byte[1024];
        AudioFormat audioFormat = getAudioFormat(selectedMixerInfo);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (speakersLine == null) {
            speakersLine = (SourceDataLine) AudioSystem.getLine(info);
        }
        synchronized (speakersLine) {
            if (!speakersLine.isOpen()) {
                speakersLine.open(audioFormat);
            }
            if (!speakersLine.isRunning()) {
                speakersLine.start();
            }
        }

        // Optional: Verify speakerLine is initialized
        if (speakersLine == null) {
            DataLine.Info info2 = new DataLine.Info(SourceDataLine.class, audioFormat);
            speakersLine = (SourceDataLine) AudioSystem.getLine(info2);
            speakersLine.open(audioFormat);
            speakersLine.start();
        }

        Task<Void> listenTask = new Task<>() {
            @Override
            protected Void call() {
            try {
                while (listening) {
                    // Call your method for listening
                    callController.listenVoice(speakersLine, decoder);
                }
                if (speakersLine != null) {
                    speakersLine.drain();
                    speakersLine.stop();
                    speakersLine.close();
                }
            } catch (Exception e) {
                encoder.resetState();
                decoder.resetState();
                e.printStackTrace();
            } finally {
                if (speakersLine != null) {
                    speakersLine.drain();
                    speakersLine.stop();
                    speakersLine.close();
                }
            }
                return null;
            }

            @Override
            protected void failed() {
                super.failed();
                // Handle failure if needed
                Throwable exc = getException();
            }
        };

        // Run the task in a background thread
        Thread thread = new Thread(listenTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void tryAudioFormat(float sampleRate, int bits, int channels, boolean signed, boolean endian) {
        AudioFormat format = new AudioFormat(sampleRate, bits, channels, signed, endian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (AudioSystem.isLineSupported(info)) {
            //System.out.println("Format supported" + format + " " + format.getChannels());
            supportedFormat = format;
        }
    }

    public AudioFormat getAudioFormat(Mixer.Info selectedMixerInfo) throws LineUnavailableException {
        // Use little-endian (false) — many system mixers expect PCM little-endian
        //AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);

        //tryAudioFormat(16000.0f, 16, 1, true, false); // 16 kHz, 16 bits, mono, signed, big-endian
        //tryAudioFormat(16000.0f, 16, 1, true, false); // 16 kHz, 16 bits, mono, signed, little-endian
        //tryAudioFormat(44100.0f, 16, 2, true, false); // 44.1 kHz, 16 bits, stereo, signed, big-endian
        tryAudioFormat(44100.0f, 16, 2, true, false); // 44.1 kHz, 16 bits, stereo, signed, little-endian
        // Get the selected mixer
        Mixer selectedMixer = AudioSystem.getMixer(selectedMixerInfo);
        if (line != null) {
            line.start();
            return line.getFormat();
        }

        // Find TargetDataLine for this mixer
        Line.Info[] targetLineInfos = selectedMixer.getTargetLineInfo();

        for (Line.Info info : targetLineInfos) {
            if (TargetDataLine.class.isAssignableFrom(info.getLineClass())) {
                line = (TargetDataLine) selectedMixer.getLine(info);
                break;
            }
        }

        if (line == null) {
            for (var sourceLine : selectedMixer.getSourceLineInfo()) {
                if (sourceLine instanceof Port.Info) {
                    // Cast sourceLine to Port.Info
                    Port.Info portInfo = (Port.Info) sourceLine;
                    // Get the port from the mixer
                    line = getTargetDataLineForPort(portInfo);
                    if (line == null) {
                        line = getDefaultMicrophone();
                        if (line != null) {
                            supportedFormat = line.getFormat();
                        }
                    }
                    else {
                        supportedFormat = line.getFormat();
                    }
                }
            }
        }
        return supportedFormat;
    }

    private void startAudioCapture(Mixer.Info selectedMixerInfo, String meetingId) {
        stopAudioCapture();
        capturing = true;

        // Wrap your audio capture logic inside a JavaFX Task
        Task<Void> captureTask = new Task<>() {
            @Override
            protected Void call() {
            try {
                // Use little-endian here as well
                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                Mixer selectedMixer = AudioSystem.getMixer(selectedMixerInfo);
                Line.Info[] targetLineInfos = selectedMixer.getTargetLineInfo();

                if (line != null) {
                    format = line.getFormat();
                    line.start();
                }
                else {
                    // Find TargetDataLine
                    for (Line.Info info : targetLineInfos) {
                        if (TargetDataLine.class.isAssignableFrom(info.getLineClass())) {
                            line = (TargetDataLine) selectedMixer.getLine(info);
                            break;
                        }
                    }

                    if (line == null) {
                        for (var sourceLine : selectedMixer.getSourceLineInfo()) {
                            if (sourceLine instanceof Port.Info) {
                                // Cast sourceLine to Port.Info
                                Port.Info portInfo = (Port.Info) sourceLine;
                                // Get the port from the mixer
                                line = getTargetDataLineForPort(portInfo);
                                if (line == null) {
                                    line = getDefaultMicrophone();
                                    if (line != null) {
                                        format = line.getFormat();
                                    }
                                } else {
                                    format = line.getFormat();
                                }
                            }
                        }
                    }
                }
                if (line != null) {
                    if (!line.isOpen()) {
                        line.open(format);
                        line.start();
                    }

                    if (speakersLine == null) {
                        speakersLine = AudioSystem.getSourceDataLine(format);
                        speakersLine.open(format);
                    }
                    if (!speakersLine.isRunning()) {
                        speakersLine.start();
                    }

                    // OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.AUDIO);
                    int frameSize = 960; // samples per frame
                    byte[] readBuffer = new byte[frameSize * 2]; // 2 bytes per sample

                    byte[][] leftoverBytes = { new byte[0] };

                    while (!Thread.currentThread().isInterrupted() && capturing) {
                        int bytesRead = line.read(readBuffer, 0, readBuffer.length);
                        if (bytesRead == -1) break;

                        // Concatenate leftover bytes with new read
                        byte[] combinedBytes = new byte[leftoverBytes[0].length + bytesRead];
                        System.arraycopy(leftoverBytes[0], 0, combinedBytes, 0, leftoverBytes[0].length);
                        System.arraycopy(readBuffer, 0, combinedBytes, leftoverBytes[0].length, bytesRead);

                        int totalBytes = combinedBytes.length;
                        int processBytes = totalBytes - (totalBytes % 2);

                        // Convert bytes to PCM samples
                        short[] pcmSamples = new short[processBytes / 2];
                        ByteBuffer bb = ByteBuffer.wrap(combinedBytes, 0, processBytes);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        for (int i = 0; i < pcmSamples.length; i++) {
                            pcmSamples[i] = bb.getShort();
                        }

                        int offset = 0;
                        while (offset + frameSize <= pcmSamples.length) {
                            short[] frame = Arrays.copyOfRange(pcmSamples, offset, offset + frameSize);
                            byte[] compressed = encoder.encode(frame);

                            byte[] finalBuffer = applyAGC(compressed, compressed.length);
                            double normalizedVolume = rmsToProgress(rms);
                            double loudThreshold = 0.01;
                            if (normalizedVolume > loudThreshold) {
                                //System.out.println("LOUD");
                                // Send voice
                                Voice voice = new Voice();
                                voice.setAudio(Arrays.copyOf(finalBuffer, finalBuffer.length));
                                voice.setMeetingId(meetingId);
                                voice.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());
                                callService.talk(voice);
                                offset += frameSize;
                            } else {
                                // Skip sending if below threshold
                                offset += frameSize;
                            }
                            // encoder.resetState();
                        }

                        // Save remaining samples as leftover
                        int remainingSamples = pcmSamples.length - offset;
                        ByteBuffer buffer = ByteBuffer.allocate(remainingSamples * 2);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        for (int i = offset; i < pcmSamples.length; i++) {
                            buffer.putShort(pcmSamples[i]);
                        }
                        leftoverBytes[0] = buffer.array();
                    }

                    // encoder.close();

                    if (line != null) {
                        line.stop();
                        line.close();
                    }
                    if (speakersLine != null) {
                        speakersLine.drain();
                        speakersLine.stop();
                        speakersLine.close();
                    }
                }
            } catch (Exception e) {
                encoder.resetState();
                decoder.resetState();
                e.printStackTrace();
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
                if (speakersLine != null) {
                    speakersLine.drain();
                    speakersLine.stop();
                    speakersLine.close();
                }
            }
                return null;
            }
        };

        // Bind the task lifecycle to the JavaFX thread
        Thread thread = new Thread(captureTask);
        thread.setDaemon(true);
        thread.start();

        // Save reference if needed
        this.captureThread = thread;
    }

    private byte[] applyAGC(byte[] buffer, int bytesRead) {
        int length = bytesRead;
        if (length % 2 != 0) {
            length -= 1; // ignore last byte if odd
        }

        // Safety check
        if (length < 2) {
            return buffer; // Not enough data to process
        }

        rms = calculateRMS(buffer, length);

        double targetRMS = 0.02;
        double gain = targetRMS / (rms + 1e-6);
        gain = Math.min(gain, 10.0);
        gain = Math.max(gain, 1.0);

        for (int i = 0; i < length; i += 2) {
            // Confirm indices are valid
            if (i + 1 >= length) break;

            short sample = (short) ((buffer[i] & 0xff) | (buffer[i + 1] << 8));
            int amplifiedSample = (int) (sample * gain);
            amplifiedSample = Math.min(Math.max(amplifiedSample, Short.MIN_VALUE), Short.MAX_VALUE);

            buffer[i] = (byte) (amplifiedSample & 0xff);
            buffer[i + 1] = (byte) (amplifiedSample >> 8);
        }
        return buffer;
    }

    public void stopAudioCapture() {
        capturing = false;
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopReceivingAndPlaying() {
        listening = false;
        if (listeningFuture != null) {
            try {
                listeningFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void supportedHeadPhone(Line line, Mixer.Info selectedMixerInfo, CallController callController) throws LineUnavailableException, IOException {
        boolean controlSupported = false;
        FloatControl volumeControl = null;

        if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            controlSupported = true;
            volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
        } else if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            controlSupported = true;
            volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        }
        // In case you have headphone + mic
        else {
            for (Control control : line.getControls()) {
                controlSupported = true;
                if (control instanceof CompoundControl) {
                    CompoundControl compControl = (CompoundControl) control;

                    // Access member controls
                    Control[] memberControls = compControl.getMemberControls();

                    for (Control member : memberControls) {
                        // Look for FloatControl or other volume-related control
                        if (member instanceof FloatControl) {
                            volumeControl = (FloatControl) member;

                            // Set volume to a desired level within min/max
                            float newVolume = (volumeControl.getMaximum() + volumeControl.getMinimum()) / 2;
                            volumeControl.setValue(newVolume);
                        }
                    }
                }
            }
        }

        if (controlSupported && volumeControl != null) {
            float currentValue = volumeControl.getValue();
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float normalized = (currentValue - min) / (max - min);
            receiveAndPlay(selectedMixerInfo, callController);
            /*javafx.application.Platform.runLater(() -> {
                volumeSlider.setDisable(false);
                volumeSlider.setValue(normalized);
            });*/
        } else {
            // No support for volume control
            /*javafx.application.Platform.runLater(() -> {
                volumeSlider.setDisable(true);
            });*/
        }
    }

    private void startVolumeMonitoring(Mixer.Info mixerInfo, CallController callController) {
        // Stop previous monitoring
        if (volumeMonitorTimeline != null) {
            volumeMonitorTimeline.stop();
            volumeMonitorTimeline = null;
        }
        if (currentLine != null && currentLine.isOpen()) {
            currentLine.close();
        }

        currentLine = null;

        Line line = null;
        try {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo();
            if (sourceLines.length > 0) {
                line = (Line) mixer.getLine(sourceLines[0]);
                if (!line.isOpen()) {
                    line.open();
                }
                currentLine = line;


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (currentLine != null) {
            volumeMonitorTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
                try {
                    if (currentLine != null && currentLine.isOpen()) {
                        supportedHeadPhone(currentLine, mixerInfo, callController);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }));
            volumeMonitorTimeline.setCycleCount(Timeline.INDEFINITE);
            volumeMonitorTimeline.play();
        }
    }
}
