package com.voicechat.client.call.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.call.component.AudioDeviceComponent;
import com.voicechat.client.call.component.CameraComponent;
import com.voicechat.client.call.component.ScreenShareComponent;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.pojo.Camera;
import org.shared.pojo.Voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

public class CallController {

    private final AudioDeviceComponent audioDeviceComponent = new AudioDeviceComponent();

    private final CameraComponent cameraComponent = new CameraComponent();

    private final CallService callService = new CallService();

    private final ScreenShareComponent screenShareComponent = new ScreenShareComponent();

    @FXML
    private ImageView cameraImageView;
    @FXML
    private VBox callUsers;
    @FXML
    private GridPane callUsersGrid;
    @FXML
    private ImageView shareScreen;
    @FXML
    private StackPane imageStackPane;

    @FXML
    public void initialize() {
        System.out.println("go initialize");
    }

    public void initData(boolean microphoneCut, boolean isCameraActive,
                         Mixer.Info selectedHeadMixerInfo, Mixer.Info selectedMicMixerInfo,
                         AudioFormat audioFormat, String meetingId, Stage stage) {
        audioDeviceComponent.setAudioDevice(microphoneCut, selectedHeadMixerInfo, selectedMicMixerInfo,
                audioFormat, meetingId, this);
        cameraComponent.sendFrames(meetingId, isCameraActive);
        cameraComponent.receiveAndDisplay(cameraImageView, meetingId, this);
        screenShareComponent.setScreenShareButton(shareScreen, imageStackPane);
        exit(stage);
    }

    public void readCamera() throws IOException {
        var responses = Listener.getServerReader()
                .getServerResponseBySpecificField("camera-" + UserSession.INSTANCE.getUser().getEmailAddress());
        if (CollectionUtils.isNotEmpty(responses)) {
            for (ServerResponse response : responses) {
                if (response != null) {
                    if (response.getServerResponseMessage() == ServerResponseMessage.HAS_CAMERA) {
                        if (response.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                            Camera camera = objectMapper.readValue(response.getBinaryPayload(), Camera.class);
                            byte[] cameraData = callService.decompress(camera.getFrames());
                            cameraComponent.setCameraOnNode(camera, callUsersGrid, cameraData);
                        }
                    }
                }
            }
        }
    }

    public void listenVoice(SourceDataLine speakerLine) throws IOException {
        var responses = Listener.getServerReader()
                .getServerResponseBySpecificField("voice-" + UserSession.INSTANCE.getUser().getEmailAddress());
        if (CollectionUtils.isNotEmpty(responses)) {
            for (ServerResponse response : responses) {
                if (response != null) {
                    if (response.getServerResponseMessage() == ServerResponseMessage.IS_TALKING) {
                        if (response.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                            Voice voice = objectMapper.readValue(response.getBinaryPayload(), Voice.class);
                            byte[] audioBytes = callService.decompress(voice.getAudio());
                            if (speakerLine != null && speakerLine.isOpen()) {
                                // Write audio bytes to speaker line
                                speakerLine.write(audioBytes, 0, audioBytes.length);
                                speakerLine.drain();
                            }
                            // Optionally keep the line open or close after each response
                            // speakerLine.close(); // Uncomment if you want to close after each
                        }
                    }
                }
            }
        }
    }

    public void exit(Stage stage) {
        stage.setOnCloseRequest(windowEvent -> {
            audioDeviceComponent.stopReceivingAndPlaying();
            audioDeviceComponent.stopAudioCapture();
            cameraComponent.stopSend();
            cameraComponent.stopReceive();
        });
    }
}
