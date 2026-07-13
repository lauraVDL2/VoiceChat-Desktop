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

public class AudioDeviceComponent extends AbstractAudioDevice {
    private boolean microphoneCut = true;
    private Timeline volumeMonitorTimeline;
    private Line currentLine; // Keep track of the current line to close/dispose
    private SourceDataLine speakerLine;

    private volatile boolean capturing = false;
    private volatile boolean listening = false;
    private Thread captureThread;
    private short[] leftoverSamples = new short[0];
    private CompletableFuture<Void> listeningFuture;
    private double rms = 0;

    private final CallService callService = new CallService();

    public void setAudioDevice(boolean microphoneCut, Mixer.Info selectedHeadMixerInfo, Mixer.Info selectedMicMixerInfo,
                               AudioFormat audioFormat, String meetingId, CallController callController) {
        this.microphoneCut = microphoneCut;
        startVolumeMonitoring(selectedHeadMixerInfo, callController);
        startAudioCapture(selectedMicMixerInfo, meetingId);
    }

    public void receiveAndPlay(Mixer.Info selectedMixerInfo, CallController callController) throws IOException, LineUnavailableException {
        listening = true;

        Task<Void> listenTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    byte[] buffer = new byte[1024];
                    AudioFormat audioFormat = getAudioFormat(selectedMixerInfo);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                    speakerLine = (SourceDataLine) AudioSystem.getLine(info);
                    speakerLine.open(audioFormat);
                    speakerLine.start();

                    // Optional: Verify speakerLine is initialized
                    if (speakerLine == null) {
                        DataLine.Info info2 = new DataLine.Info(SourceDataLine.class, audioFormat);
                        speakerLine = (SourceDataLine) AudioSystem.getLine(info2);
                        speakerLine.open(audioFormat);
                        speakerLine.start();
                    }

                    while (listening) {
                        // Call your method for listening
                        callController.listenVoice(speakerLine);
                    }
                    if (speakerLine != null) {
                        speakerLine.drain();
                        speakerLine.stop();
                        speakerLine.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (speakerLine != null) {
                        speakerLine.drain();
                        speakerLine.stop();
                        speakerLine.close();
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

    public AudioFormat getAudioFormat(Mixer.Info selectedMixerInfo) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);

        // Get the selected mixer
        Mixer selectedMixer = AudioSystem.getMixer(selectedMixerInfo);
        TargetDataLine line = null;

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
                            format = line.getFormat();
                        }
                    }
                    else {
                        format = line.getFormat();
                    }
                }
            }
        }
        return format;
    }

    private void startAudioCapture(Mixer.Info selectedMixerInfo, String meetingId) {
        stopAudioCapture();
        capturing = true;

        // Wrap your audio capture logic inside a JavaFX Task
        Task<Void> captureTask = new Task<>() {
            @Override
            protected Void call() {
                TargetDataLine line = null;
                SourceDataLine speakersLine = null;
                try {
                    AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
                    Mixer selectedMixer = AudioSystem.getMixer(selectedMixerInfo);
                    Line.Info[] targetLineInfos = selectedMixer.getTargetLineInfo();

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
                    if (line != null) {
                        line.open(format);
                        line.start();

                        speakersLine = AudioSystem.getSourceDataLine(format);
                        speakersLine.open(format);
                        speakersLine.start();

                        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.AUDIO);
                        int frameSize = 960; // samples per frame
                        byte[] readBuffer = new byte[frameSize * 2]; // 2 bytes per sample

                        byte[][] leftoverBytes = { new byte[0] };

                        while (!isCancelled() && capturing) {
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
                            bb.order(ByteOrder.BIG_ENDIAN);
                            for (int i = 0; i < pcmSamples.length; i++) {
                                pcmSamples[i] = bb.getShort();
                            }

                            int offset = 0;
                            if (offset + frameSize <= pcmSamples.length) {
                            //while (offset + frameSize <= pcmSamples.length) {
                                short[] frame = Arrays.copyOfRange(pcmSamples, offset, offset + frameSize);
                                byte[] compressed = encoder.encode(frame);

                                byte[] finalBuffer = applyAGC(compressed, compressed.length);
                                double normalizedVolume = rmsToProgress(rms);
                                double loudThreshold = 0.01;
                                if (normalizedVolume > loudThreshold) {
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
                            }

                            // Save remaining samples as leftover
                            int remainingSamples = pcmSamples.length - offset;
                            ByteBuffer buffer = ByteBuffer.allocate(remainingSamples * 2);
                            buffer.order(ByteOrder.BIG_ENDIAN);
                            for (int i = offset; i < pcmSamples.length; i++) {
                                buffer.putShort(pcmSamples[i]);
                            }
                            leftoverBytes[0] = buffer.array();
                        }

                        encoder.close();

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
                    e.printStackTrace();
                }
                finally {
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
        // Ensure bytesRead is even
        int length = bytesRead;
        if (length % 2 != 0) {
            length -= 1; // ignore the last byte if odd
        }
        rms = calculateRMS(buffer, length);

        double targetRMS = 0.02; // desired volume level
        double gain = targetRMS / (rms + 1e-6); // avoid division by zero

        // Clamp gain to prevent excessive amplification
        gain = Math.min(gain, 10.0); // maximum gain factor
        gain = Math.max(gain, 1.0);  // ensure gain is at least 1 for louder effect

        // Apply gain to each sample
        for (int i = 0; i < length; i += 2) {
            short sample = (short) ((buffer[i] & 0xff) | (buffer[i + 1] << 8));
            int amplifiedSample = (int) (sample * gain);

            // Clamp to avoid overflow
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
