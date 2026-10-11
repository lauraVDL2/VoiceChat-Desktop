package com.voicechat.client.mainpage.component;

import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.mainpage.controller.HeaderController;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.shared.entity.User;

import java.util.List;

public class UserSearchComponent {

    public void searchUserComponents(
            HeaderController headerController,
            Pane searchPane,
            List<User> users
    ) {
        Platform.runLater(() -> {
            VBox results = new VBox(4);
            results.getStyleClass().add("search-results");

            for (User user : users) {
                results.getChildren().add(
                        createUserRow(headerController, user)
                );
            }

            searchPane.getChildren().setAll(results);
        });
    }

    private HBox createUserRow(
            HeaderController headerController,
            User user
    ) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("search-user-row");

        ImageView avatar = new ImageView();
        avatar.setFitWidth(38);
        avatar.setFitHeight(38);
        avatar.setPreserveRatio(true);
        avatar.setClip(new Circle(19, 19, 19));
        avatar.getStyleClass().add("search-user-avatar");

        var avatarResource =
                VoiceChatApplication.class.getResourceAsStream(
                        "images/default-avatar.png"
                );

        if (avatarResource != null) {
            avatar.setImage(new Image(avatarResource));
        }

        Label name = new Label(user.getDisplayName());
        name.getStyleClass().add("search-user-name");

        Label email = new Label(user.getEmailAddress());
        email.getStyleClass().add("search-user-email");

        VBox details = new VBox(3, name, email);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label arrow = new Label("›");
        arrow.getStyleClass().add("search-user-arrow");

        row.getChildren().addAll(avatar, details, arrow);

        Tooltip.install(
                row,
                new Tooltip("Start a conversation with " + user.getDisplayName())
        );

        row.setOnMouseClicked(event ->
                headerController.startConversation(user)
        );

        return row;
    }
}