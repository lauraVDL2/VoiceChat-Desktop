package com.voicechat.client.call.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.call.component.AudioDeviceComponent;
import com.voicechat.client.call.component.CameraComponent;
import com.voicechat.client.call.component.ScreenShareComponent;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import de.maxhenkel.opus4j.OpusDecoder;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import org.shared.pojo.ScreenShare;
import org.shared.pojo.Voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
        screenShareComponent.setScreenShareButton(shareScreen, imageStackPane, meetingId);
        screenShareComponent.receiveAndDisplay(this);
        exit(stage);
    }

    public void readScreen() {
        //Task<Void> imageUpdateTask = new Task<>() {
            //@Override
           //protected Void call() throws Exception {
        try {
            var responses = Listener.getServerReader()
                    .getServerResponseBySpecificField("screen-" + UserSession.INSTANCE.getUser().getEmailAddress());
            if (CollectionUtils.isNotEmpty(responses)) {
                for (ServerResponse response : responses) {
                    if (response != null &&
                            response.getServerResponseMessage() == ServerResponseMessage.IS_SCREEN_SHARING &&
                            response.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {

                        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                        ScreenShare screenShare = objectMapper.readValue(response.getBinaryPayload(), ScreenShare.class);
                        byte[] screenData = callService.decompress(screenShare.getFrames());
                        Platform.runLater(() -> {
                            try {
                                screenShareComponent.addScreenToNode(imageStackPane, screenData);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
               // return null;
//            }
//        };
//
//        Thread thread = new Thread(imageUpdateTask);
//        thread.setDaemon(true);
//        thread.start();
    }

    public void readCamera() throws Exception {
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

    public void listenVoice(SourceDataLine speakerLine, OpusDecoder decoder) throws Exception {
        var responses = Listener.getServerReader()
                .getServerResponseBySpecificField("voice-" + UserSession.INSTANCE.getUser().getEmailAddress());
        ByteArrayOutputStream batchBuffer = new ByteArrayOutputStream();
        if (CollectionUtils.isNotEmpty(responses)) {
            for (ServerResponse response : responses) {
                if (response != null) {
                    if (response.getServerResponseMessage() == ServerResponseMessage.IS_TALKING) {
                        if (response.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                            Voice voice = objectMapper.readValue(response.getBinaryPayload(), Voice.class);
                            byte[] audioBytes = callService.decompressVoice(voice.getAudio(), decoder);
                            if (audioBytes != null && audioBytes.length > 0) {
                                batchBuffer.write(audioBytes);
                            }
                        }
                    }
                }
            }
            byte[] batchedData = batchBuffer.toByteArray();
            if (batchedData.length > 0 && speakerLine != null && speakerLine.isOpen()) {
                speakerLine.write(batchedData, 0, batchedData.length);
                speakerLine.drain(); // Call drain once after batch
            }
            batchBuffer.close();
        }
    }

    public void exit(Stage stage) {
        stage.setOnCloseRequest(windowEvent -> {
            audioDeviceComponent.stopReceivingAndPlaying();
            audioDeviceComponent.stopAudioCapture();
            cameraComponent.stopSend();
            cameraComponent.stopReceive();
            screenShareComponent.stopSend();
            screenShareComponent.stopReceive();
        });
    }
}
