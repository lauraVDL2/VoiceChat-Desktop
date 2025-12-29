package com.voicechat.client.call.component;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

public class AudioDeviceSelectorComponent {

    public void setAudioDeviceChoice(VBox vBox) {
        ComboBox<String> micComboBox = new ComboBox<>();
        ComboBox<String> headphonesComboBox = new ComboBox<>();
        micComboBox.setPrefWidth(250);
        headphonesComboBox.setPrefWidth(250);
        micComboBox.getStyleClass().add("callComboBox");
        headphonesComboBox.getStyleClass().add("callComboBox");

        // Populate microphones
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLines = mixer.getTargetLineInfo(); // input lines
            if (targetLines != null && targetLines.length > 0) {
                micComboBox.getItems().add(mixerInfo.getName() + "-" + mixerInfo.getDescription());
            }
        }

        // Populate output devices (headphones)
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo(); // output lines
            if (sourceLines != null && sourceLines.length > 0) {
                headphonesComboBox.getItems().add(mixerInfo.getName() + "-" + mixerInfo.getDescription());
            }
        }
        vBox.getChildren().addAll(micComboBox, headphonesComboBox);
    }
}
