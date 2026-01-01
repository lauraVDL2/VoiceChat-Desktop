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
import org.apache.commons.lang3.StringUtils;

import javax.sound.sampled.*;

public class AudioDeviceSelectorComponent extends AbstractAudioDevice {

    private final MarginComponent marginComponent = new MarginComponent();

    private Slider volumeSlider;
    private boolean microphoneCut = true;
    private Timeline volumeMonitorTimeline;
    private Line currentLine; // Keep track of the current line to close/dispose

    private volatile boolean capturing = false;
    private Thread captureThread;

    private ProgressBar progressBar;
    private ComboBox<String> micComboBox;

    private Mixer.Info selectedHeadMixerInfo;
    private Mixer.Info selectedMicMixerInfo;
    private AudioFormat audioFormat;

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
                micComboBox.getItems().add(mixerInfo.getDescription());
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
                headphonesComboBox.getItems().add(mixerInfo.getDescription());
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
                // ComboBox contains String descriptions
                String description = (String) newVal;
                // find the corresponding MixerInfo
                for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                    if (StringUtils.equalsIgnoreCase(info.getDescription(), description)) {
                        selectedHeadMixerInfo = info;
                        startVolumeMonitoring(selectedHeadMixerInfo);
                        break;
                    }
                }
            }
            else {
                String description = (String) newVal;
                // find the corresponding MixerInfo
                for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                    if (StringUtils.equalsIgnoreCase(info.getDescription(), description)) {
                        selectedHeadMixerInfo = info;
                        startVolumeMonitoring(selectedHeadMixerInfo);
                        break;
                    }
                }
            }
        });

        micComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // ComboBox contains String descriptions
                String description = (String) newVal;
                // find the corresponding MixerInfo
                for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                    if (StringUtils.equalsIgnoreCase(info.getDescription(), description)) {
                        selectedMicMixerInfo = info;
                        break;
                    }
                }
            }
            else {
                // ComboBox contains String descriptions
                String description = (String) oldVal;
                // find the corresponding MixerInfo
                for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                    if (StringUtils.equalsIgnoreCase(info.getDescription(), description)) {
                        selectedMicMixerInfo = info;
                        break;
                    }
                }
            }
        });

        // Start monitoring with the initially selected device
        int initialIndex = headphonesComboBox.getSelectionModel().getSelectedIndex();
        if (initialIndex >= 0 && initialIndex < AudioSystem.getMixerInfo().length) {
            startVolumeMonitoring(AudioSystem.getMixerInfo()[initialIndex]);
        }
    }

    private void startAudioCapture() {
        // Stop previous capture if any
        stopAudioCapture();

        /*int selectedIndex = micComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= AudioSystem.getMixerInfo().length) {
            System.out.println("Invalid microphone selection");
            return;
        }

        selectedMicMixerInfo = AudioSystem.getMixerInfo()[selectedIndex];*/
        System.out.println("descr = " + selectedMicMixerInfo.getDescription() + " mic name = " + selectedMicMixerInfo.getName());

        capturing = true;

        captureThread = new Thread(() -> {
            try {
                // Define audio format
                audioFormat = new AudioFormat(44100.0f, 16, 1, true, true);

                // Get the selected mixer
                Mixer selectedMixer = AudioSystem.getMixer(selectedMicMixerInfo);
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
                                    audioFormat = line.getFormat();
                                }
                            }
                            else {
                                audioFormat = line.getFormat();
                            }
                        }
                    }
                }
                if (line != null) {
                    line.open(audioFormat);
                    line.start();
                    SourceDataLine speakersLine;
                    speakersLine = AudioSystem.getSourceDataLine(audioFormat);
                    speakersLine.open(audioFormat);
                    speakersLine.start();


                    byte[] buffer = new byte[1024];

                    while (capturing) {
                        int bytesRead = line.read(buffer, 0, buffer.length);
                        double rms = calculateRMS(buffer, bytesRead);
                        double normalizedVolume = rmsToProgress(rms);
                        /*// Send data to speakers (monitoring)
                        if (speakersLine != null && bytesRead > 0) {
                            speakersLine.write(buffer, 0, bytesRead);
                        }*/
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

    public Mixer.Info getSelectedHeadMixerInfo() {
        return selectedHeadMixerInfo;
    }

    public Mixer.Info getSelectedMicMixerInfo() {
        return selectedMicMixerInfo;
    }

    public boolean isMicrophoneCut() {
        return microphoneCut;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

}
