package com.voicechat.client.mainpage.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AvatarComponent {


    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void onlineCircle(StackPane stackPane, Color color) {
        if (stackPane != null) {
            Platform.runLater(() -> {
                boolean exists = false;
                for (var child : stackPane.getChildren()) {
                    // We do not add the same circle twice. If exists, we only change the color
                    if (child instanceof Circle) {
                        ((Circle) child).setFill(color);
                        exists = true;
                    }
                }
                if (!exists) {
                    Circle onlineIndicator = new Circle(5);
                    onlineIndicator.setFill(color);
                    StackPane.setAlignment(onlineIndicator, Pos.CENTER_LEFT);
                    StackPane.setMargin(onlineIndicator, new Insets(20, 0, 0, 2));

                    stackPane.getChildren().add(onlineIndicator);
                }
            });
        }
    }

    public void modifyUsersOnlineMessage(GridPane gridPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            BorderPane mainPane = (BorderPane) gridPane.lookup("#mainPane");
            VBox messageContentBox = (VBox) mainPane.lookup("#messageContentBox");
            var nodes = new ArrayList<>(messageContentBox.lookupAll(".emailAddressMessage"));
            var stackPanes = new ArrayList<>(messageContentBox.lookupAll(".stackPaneMessage"));
            for (int i = 0; i < nodes.size(); i++) {
                String emailAddress = ((Label) nodes.get(i)).getText();
                StackPane stackPane = (StackPane) stackPanes.get(i);
                var onlineUsers = serverResponse.getServerInformation().getOnlineUsers();
                onlineCircle(stackPane, Color.GREY);
                for (var onlineUser : onlineUsers.entrySet()) {
                    if (StringUtils.equals(onlineUser.getKey(), emailAddress)) {
                        if (onlineUser.getValue() == UserSessionStatus.ONLINE) {
                            onlineCircle(stackPane, Color.GREEN);
                        }
                    }
                }
            }
        });
    }

    public void modifyUsersOnlineConversationList(GridPane gridPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            VBox leftPane = (VBox) gridPane.lookup("#leftPane");
            if (leftPane != null) {
                var nodes = new ArrayList<>(leftPane.lookupAll(".displayNamesLabel"));
                for (int i = 0; i < nodes.size(); i++) {
                    String emailAddresses = nodes.get(i).getId();
                    // We only display the online status in the conversations if it's not a group
                    if (emailAddresses.split(",").length == 1) {
                        StackPane stackPane = (StackPane) new ArrayList<>(
                                leftPane.lookupAll(".stackAvatarConversationList")).get(i);
                        var onlineUsers = serverResponse.getServerInformation().getOnlineUsers();
                        onlineCircle(stackPane, Color.GREY);
                        for (var onlineUser : onlineUsers.entrySet()) {
                            if (StringUtils.equals(onlineUser.getKey(), emailAddresses)) {
                                if (onlineUser.getValue() == UserSessionStatus.ONLINE) {
                                    onlineCircle(stackPane, Color.GREEN);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
