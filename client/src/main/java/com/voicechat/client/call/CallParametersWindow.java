package com.voicechat.client.call;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.call.component.AudioDeviceSelectorComponent;
import com.voicechat.client.call.component.CameraSelectorComponent;
import com.voicechat.client.call.controller.CallController;
import com.voicechat.client.call.service.CallService;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.common.component.MarginComponent;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.pojo.VoiceChatEvent;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class CallParametersWindow {

    private final AudioDeviceSelectorComponent audioDeviceSelectorComponent = new AudioDeviceSelectorComponent();

    private final CameraSelectorComponent cameraSelectorComponent = new CameraSelectorComponent();

    private final MarginComponent marginComponent = new MarginComponent();

    private final CallService callService = new CallService();

    public void setWindow(VoiceChatEvent event) {
        Stage stage = new Stage();
        stage.setTitle(event.getSubject());
        StackPane pane = new StackPane();
        HBox root = new HBox();
        root.setAlignment(Pos.CENTER);
        VBox rootVBox = new VBox();
        rootVBox.setAlignment(Pos.CENTER);
        Label cameraLabel = new Label("Camera");
        cameraLabel.getStyleClass().add("callTestLabel");
        VBox cameraBox = new VBox();
        cameraBox.setAlignment(Pos.CENTER);

        ImageView cameraView = new ImageView();
        cameraView.getStyleClass().add("cameraViewTest");
        cameraView.setFitHeight(300);
        cameraView.setFitWidth(400);
        ToggleSwitch toggleSwitch = new ToggleSwitch();
        toggleSwitch.setSelected(false);
        toggleSwitch.getStyleClass().add("callTestSwitch");
        toggleSwitch.setPrefWidth(400);
        Region cameraVerticalMargin1 = new Region();
        Region cameraVerticalMargin2 = new Region();
        cameraVerticalMargin1.setPrefHeight(15);
        cameraVerticalMargin2.setPrefHeight(15);
        cameraBox.getChildren().addAll(cameraLabel, cameraVerticalMargin1, cameraView, cameraVerticalMargin2, toggleSwitch);
        toggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                cameraSelectorComponent.startCamera(cameraView);
            }
            else {
                cameraSelectorComponent.stop();
                cameraView.setImage(null);
            }
        });

        VBox audioBox = new VBox();
        audioBox.setAlignment(Pos.CENTER);

        audioDeviceSelectorComponent.setAudioDeviceChoice(audioBox);
        Region horizontalMargin = new Region();
        horizontalMargin.setPrefWidth(15);
        root.getChildren().addAll(cameraBox, horizontalMargin, audioBox);
        Button join = new Button("Join");
        join.getStyleClass().add("callEventButton");
        rootVBox.getChildren().addAll(root, marginComponent.initVerticalMargin(15), join);
        pane.getChildren().add(rootVBox);

        joinCall(join, event, stage);

        Scene scene = new Scene(pane, Screen.getPrimary().getVisualBounds().getWidth() - 200,
                Screen.getPrimary().getVisualBounds().getHeight() - 200);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        exit(stage);
    }

    public void exit(Stage stage) {
        stage.setOnCloseRequest(windowEvent -> {
            audioDeviceSelectorComponent.stopAudioCapture();
            cameraSelectorComponent.stop();
        });
    }

    public void joinCall(Button join, VoiceChatEvent voiceChatEvent, Stage stage) {
        join.setOnMouseClicked((e) -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    voiceChatEvent.setUserEmailAddress(UserSession.INSTANCE.getUser().getEmailAddress());
                    return callService.connect(voiceChatEvent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }).thenAcceptAsync(serverResponse -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MEETING_CONNECTED) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            Platform.runLater(() -> {
                                FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/call.fxml"));
                                try {
                                    Parent parent = loader.load();
                                    CallController callController = loader.getController();
                                    boolean isCameraActive = cameraSelectorComponent.isCameraActive();
                                    cameraSelectorComponent.stop();
                                    callController.initData(audioDeviceSelectorComponent.isMicrophoneCut(), isCameraActive,
                                            audioDeviceSelectorComponent.getSelectedHeadMixerInfo(),
                                            audioDeviceSelectorComponent.getSelectedMicMixerInfo(), audioDeviceSelectorComponent.getAudioFormat(),
                                            voiceChatEvent.getId(), stage);
                                    audioDeviceSelectorComponent.stopAudioCapture();
                                    Scene scene = new Scene(parent);
                                    scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/dark-theme.css").toExternalForm());
                                    stage.setScene(scene);
                                    stage.setMaximized(true);
                                    stage.show();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    }
                }
            });
        });
    }
}
