package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainPageScheduler {

    public void schedule(GridPane gridPane) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchLoggedUsers(gridPane);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void fetchLoggedUsers(GridPane gridPane) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = new Message(MessageType.ONLINE_USERS_FETCH, null);
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();

        if (StringUtils.isNotBlank(serverInLine)) {
            ServerResponse serverResponse = objectMapper.readValue(serverInLine, ServerResponse.class);
            if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.ONLINE_USERS_FETCHED) {
                    modifyUsersOnlineUi(gridPane, serverResponse);
                }
            }
        }
    }

    public void modifyUsersOnlineUi(GridPane gridPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            VBox leftPane = (VBox) gridPane.lookup("#leftPane");
            if (leftPane != null) {
                var children = leftPane.getChildren();
                for (var child : children) {
                    if (child instanceof VBox) {
                        VBox mainvBox = (VBox) child;
                        var vBoxChildren = mainvBox.getChildren();
                        HBox hBox = (HBox) vBoxChildren.get(1);
                        var hBoxChildren = hBox.getChildren();
                        VBox vBox2 = (VBox) hBoxChildren.get(0);
                        VBox vBox = (VBox) hBoxChildren.get(1);
                        var vBoxChildren2 = vBox.getChildren();
                        VBox displayNames = (VBox) vBoxChildren2.get(0);
                        String emailAddresses = displayNames.getId();
                        // We only display the online status in the conversations if it's not a group
                        if (emailAddresses.split(",").length == 1) {
                            var vBox2Children = vBox2.getChildren();
                            StackPane stackPane = (StackPane) vBox2Children.get(0);
                            var onlineUsers = serverResponse.getServerInformation().getOnlineUsers();
                            for (var onlineUser : onlineUsers.entrySet()) {
                                if (StringUtils.equals(onlineUser.getKey(), emailAddresses)) {
                                    if (StringUtils.equals(onlineUser.getValue(), "ONLINE")) {
                                        Circle onlineIndicator = new Circle(5);
                                        onlineIndicator.setFill(Color.GREEN);
                                        onlineIndicator.setStroke(Color.BLACK);
                                        StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_LEFT);
                                        StackPane.setMargin(onlineIndicator, new Insets(0, 0, 5, 5));
                                        stackPane.getChildren().add(onlineIndicator);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
