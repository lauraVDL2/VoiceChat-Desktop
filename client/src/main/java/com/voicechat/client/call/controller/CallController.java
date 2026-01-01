package com.voicechat.client.call.controller;

import com.voicechat.client.call.component.AudioDeviceComponent;
import javafx.fxml.FXML;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

public class CallController {

    private final AudioDeviceComponent audioDeviceComponent = new AudioDeviceComponent();

    @FXML
    public void initialize() {
        System.out.println("go initialize");
    }

    public void initData(boolean microphoneCut, Mixer.Info selectedHeadMixerInfo, Mixer.Info selectedMicMixerInfo,
                         AudioFormat audioFormat, String meetingId) {
        System.out.println("go init data");
        audioDeviceComponent.setAudioDevice(microphoneCut, selectedHeadMixerInfo, selectedMicMixerInfo, audioFormat, meetingId);
    }
}
