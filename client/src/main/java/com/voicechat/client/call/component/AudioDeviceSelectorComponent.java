package com.voicechat.client.call.component;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.component.MarginComponent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioDeviceSelectorComponent {

    private final MarginComponent marginComponent = new MarginComponent();

    private Slider volumeSlider;
    private boolean microphoneCut = true;
    private Timeline volumeMonitorTimeline;
    private Line currentLine; // Keep track of the current line to close/dispose

    private volatile boolean capturing = false;
    private Thread captureThread;

    private ProgressBar progressBar;
    private ComboBox<String> micComboBox;

    public void setAudioDeviceChoice(VBox vBox) {
        micComboBox = new ComboBox<>();
        ComboBox<String> headphonesComboBox = new ComboBox<>();
        micComboBox.setPrefWidth(250);
        headphonesComboBox.setPrefWidth(250);
        micComboBox.getStyleClass().add("callComboBox");
        headphonesComboBox.getStyleClass().add("callComboBox");

        Label microphoneLabel = new Label("Microphone");
        microphoneLabel.getStyleClass().add("callTestLabel");

        // Populate microphones
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLines = mixer.getTargetLineInfo(); // input lines
            if (targetLines != null && targetLines.length > 0) {
                micComboBox.getItems().add(mixerInfo.getName() + "-" + mixerInfo.getDescription());
            }
        }
        micComboBox.getSelectionModel().selectFirst();

        Label headphoneLabel = new Label("Headphone");
        headphoneLabel.getStyleClass().add("callTestLabel");

        // Populate output devices (headphones)
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo(); // output lines
            if (sourceLines != null && sourceLines.length > 0) {
                headphonesComboBox.getItems().add(mixerInfo.getName() + "-" + mixerInfo.getDescription());
            }
        }
        headphonesComboBox.getSelectionModel().selectFirst();

        // Create ProgressBar for volume
        HBox boxVolume = new HBox();
        boxVolume.setAlignment(Pos.CENTER);
        volumeSlider = new Slider(0, 1, 0);
        volumeSlider.setPrefWidth(200);
        volumeSlider.setPrefHeight(20);
        volumeSlider.getStyleClass().add("volumeSlider");
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("progressMicTest");
        progressBar.setPrefWidth(200);
        boxVolume.getChildren().addAll(initAllowedMicrophone(), marginComponent.initHorizontalMargin(15), progressBar);
        vBox.getChildren().addAll(headphoneLabel, marginComponent.initVerticalMargin(10),
                headphonesComboBox, marginComponent.initVerticalMargin(10), volumeSlider, marginComponent.initVerticalMargin(10),
                microphoneLabel, marginComponent.initVerticalMargin(10), micComboBox, marginComponent.initVerticalMargin(10), boxVolume);

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {

            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                Node track = volumeSlider.lookup(".track");
                if (track != null) {
                    double pct = (t1.doubleValue() - volumeSlider.getMin()) / (volumeSlider.getMax() - volumeSlider.getMin()) * 100;
                    track.setStyle("-fx-background-color: linear-gradient(to right, #006abc, #006abc " + pct + "%, transparent " + pct + "%, transparent);");
                }
            }
        });

        // Add listener to start monitoring volume when a device is selected
        headphonesComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = headphonesComboBox.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < AudioSystem.getMixerInfo().length) {
                    Mixer.Info selectedMixerInfo = AudioSystem.getMixerInfo()[index];
                    startVolumeMonitoring(selectedMixerInfo);
                }
            }
        });

        // Start monitoring with the initially selected device
        int initialIndex = headphonesComboBox.getSelectionModel().getSelectedIndex();
        if (initialIndex >= 0 && initialIndex < AudioSystem.getMixerInfo().length) {
            startVolumeMonitoring(AudioSystem.getMixerInfo()[initialIndex]);
        }
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

    private void startAudioCapture() {
        // Stop previous capture if any
        stopAudioCapture();

        int selectedIndex = micComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= AudioSystem.getMixerInfo().length) {
            System.out.println("Invalid microphone selection");
            return;
        }

        Mixer.Info selectedMixerInfo = AudioSystem.getMixerInfo()[selectedIndex];

        capturing = true;

        captureThread = new Thread(() -> {
            try {
                // Define audio format
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

                    byte[] buffer = new byte[1024];

                    while (capturing) {
                        int bytesRead = line.read(buffer, 0, buffer.length);
                        double rms = calculateRMS(buffer, bytesRead);
                        double normalizedVolume = rmsToProgress(rms);

                        // Update UI
                        Platform.runLater(() -> progressBar.setProgress(normalizedVolume));
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

    private void stopAudioCapture() {
        capturing = false;
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private double calculateRMS(byte[] buffer, int bytesRead) {
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
    private double rmsToProgress(double rms) {
        // Adjust the denominator based on your environment
        double maxRMS = 32768; // Max for 16-bit audio
        double normalized = rms / maxRMS;
        // Optional: add smoothing or thresholding
        return Math.min(normalized, 1.0);
    }

    public ImageView initAllowedMicrophone() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(26);
        imageView.setFitHeight(26);
        imageView.getStyleClass().add("microphoneTestCall");
        Platform.runLater(() -> {
            Image image = new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/microphone-cut.png"));
            imageView.setImage(image);
            imageView.setOnMouseClicked(e -> {
                microphoneCut = !microphoneCut;
                if (microphoneCut) {
                    stopAudioCapture();
                    imageView.setImage(new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/microphone-cut.png")));
                }
                else {
                    startAudioCapture();
                    imageView.setImage(new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/microphone.png")));
                }
            });
        });
        return imageView;
    }

    private void supportedHeadPhone(Line line) {
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
            javafx.application.Platform.runLater(() -> {
                volumeSlider.setDisable(false);
                volumeSlider.setValue(normalized);
            });
        } else {
            // No support for volume control
            javafx.application.Platform.runLater(() -> {
                volumeSlider.setDisable(true);
            });
        }
    }

    private void startVolumeMonitoring(Mixer.Info mixerInfo) {
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
                        supportedHeadPhone(currentLine);
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
