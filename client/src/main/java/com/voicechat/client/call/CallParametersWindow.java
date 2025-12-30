package com.voicechat.client.call;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.call.component.AudioDeviceSelectorComponent;
import com.voicechat.client.call.component.CameraComponent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.shared.pojo.VoiceChatEvent;

public class CallParametersWindow {

    private final AudioDeviceSelectorComponent audioDeviceSelectorComponent = new AudioDeviceSelectorComponent();

    private final CameraComponent cameraComponent = new CameraComponent();

    public void setWindow(VoiceChatEvent event) {
        Stage stage = new Stage();
        stage.setTitle(event.getSubject());
        StackPane pane = new StackPane();
        HBox root = new HBox();
        root.setAlignment(Pos.CENTER);
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
                cameraComponent.startCamera(cameraView);
            }
            else {
                cameraComponent.stop();
                cameraView.setImage(null);
            }
        });

        VBox audioBox = new VBox();
        audioBox.setAlignment(Pos.CENTER);

        audioDeviceSelectorComponent.setAudioDeviceChoice(audioBox);
        Region horizontalMargin = new Region();
        horizontalMargin.setPrefWidth(15);
        root.getChildren().addAll(cameraBox, horizontalMargin, audioBox);
        pane.getChildren().add(root);

        Scene scene = new Scene(pane, Screen.getPrimary().getVisualBounds().getWidth() - 200,
                Screen.getPrimary().getVisualBounds().getHeight() - 200);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/call-window.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}
