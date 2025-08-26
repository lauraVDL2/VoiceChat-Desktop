package com.voicechat.client.mainpage.component;

import com.voicechat.client.Listener;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class AvatarComponent {

    public VBox readTargetAvatar(VBox vBox) {
        try {
            DataInputStream dataInputStream = Listener.getDataInputStream();
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] imageBytes = new byte[size];
                dataInputStream.readFully(imageBytes);
                javafx.scene.image.Image image = new Image(new ByteArrayInputStream(imageBytes));
                ImageView avatar = new ImageView();
                avatar.setFitHeight(40);
                avatar.setFitWidth(40);
                Platform.runLater(() -> {
                            avatar.setImage(image);
                            vBox.getChildren().add(avatar);
                        }
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vBox;
    }

    public ImageView readTargetAvatar() {
        ImageView avatar = new ImageView();
        try {
            DataInputStream dataInputStream = Listener.getDataInputStream();
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] imageBytes = new byte[size];
                dataInputStream.readFully(imageBytes);
                javafx.scene.image.Image image = new Image(new ByteArrayInputStream(imageBytes));
                avatar.setFitHeight(40);
                avatar.setFitWidth(40);
                Platform.runLater(() -> {
                            avatar.setImage(image);
                        }
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return avatar;
    }
}
