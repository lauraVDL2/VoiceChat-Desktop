package com.voicechat.client.call.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import de.maxhenkel.opus4j.OpusDecoder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
    private CompletableFuture<Void> listeningFuture;

    private final CallService callService = new CallService();

    public void setAudioDevice(boolean microphoneCut, Mixer.Info selectedHeadMixerInfo, Mixer.Info selectedMicMixerInfo,
                               AudioFormat audioFormat, String meetingId, CallController callController) {
        this.microphoneCut = microphoneCut;
        startVolumeMonitoring(selectedHeadMixerInfo, callController);
        startAudioCapture(selectedMicMixerInfo, meetingId);
    }


    public void receiveAndPlay(Mixer.Info selectedMixerInfo, CallController callController) throws IOException, LineUnavailableException {
        listening = true;
        // Run the listening process asynchronously
        listeningFuture = CompletableFuture.runAsync(() -> {
            try {
                byte[] buffer = new byte[1024];

                AudioFormat audioFormat = getAudioFormat(selectedMixerInfo);

                //System.out.println("audio format = " + audioFormat.toString());

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                speakerLine = (SourceDataLine) AudioSystem.getLine(info);
                speakerLine.open(audioFormat);
                speakerLine.start();

                // Ensure speakerLine is initialized properly
                if (speakerLine == null) {
                    //AudioFormat format2 = new AudioFormat(44100.0f, 16, 1, true, true);
                    DataLine.Info info2 = new DataLine.Info(SourceDataLine.class, audioFormat);
                    speakerLine = (SourceDataLine) AudioSystem.getLine(info2);
                    speakerLine.open(audioFormat);
                    speakerLine.start();
                }
                while (listening) {
                    callController.listenVoice(speakerLine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (speakerLine != null) {
                    speakerLine.drain();
                    speakerLine.close();
                }
            }
        });
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
        // Stop previous capture if any
        stopAudioCapture();

        capturing = true;

        captureThread = new Thread(() -> {
            try {
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
                if (line != null) {
                    line.open(format);
                    line.start();

                    SourceDataLine speakersLine;
                    speakersLine = AudioSystem.getSourceDataLine(format);
                    speakersLine.open(format);
                    speakersLine.start();

                    byte[] buffer = new byte[1024];

                    while (capturing) {
                        int bytesRead = line.read(buffer, 0, buffer.length);
                        //baos.write(buffer, 0, bytesRead);
                        double rms = calculateRMS(buffer, bytesRead);
                        double normalizedVolume = rmsToProgress(rms);
                        double loudThreshold = 0.001;

                        if (normalizedVolume > loudThreshold) {
                            Voice voice = new Voice();
                            voice.setAudio(Arrays.copyOf(buffer, bytesRead));
                            voice.setMeetingId(meetingId);
                            voice.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());
                            if (speakersLine != null && bytesRead > 0) {
                                callService.talk(voice);
                            }
                            /*try {
                                callService.talk(voice);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }*/
                        }

                    }

                    line.stop();
                    line.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
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
