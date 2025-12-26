package com.voicechat.client.login.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AuthenticatePopupComponent {

    public static Stage stagePopup;

    public static void showPopup(Stage stage, String code) {
        Platform.runLater(() -> {
            stagePopup = new Stage();
            stagePopup.initStyle(StageStyle.UNDECORATED);
            stagePopup.setResizable(false);
            stagePopup.initOwner(stage);
            Label label = new Label();
            label.setText("Enter the following code in your browser to authenticate fully :");
            label.getStyleClass().add("labelPopup");
            TextField textField = new TextField();
            textField.setAlignment(Pos.CENTER);
            textField.getStyleClass().add("textfieldPopup");
            textField.setText(code);
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(20));
            vbox.getStyleClass().add("vboxPopup");
            vbox.getChildren().addAll(label, textField);
            Scene scene = new Scene(vbox);
            stagePopup.setScene(scene);
            stagePopup.toFront();
            stagePopup.show();
        });
    }

    public static void closePopup() {
        Platform.runLater(() -> {
            if (stagePopup != null) {
                stagePopup.close();
            }
        });
    }
}
