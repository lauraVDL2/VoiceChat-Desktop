package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OnlineUsersScheduler {

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
                    StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(onlineIndicator, new Insets(0, 0, 5, 5));

                    stackPane.getChildren().add(onlineIndicator);
                }
            });
        }
    }


    public void modifyUsersOnlineUi(GridPane gridPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            VBox leftPane = (VBox) gridPane.lookup("#leftPane");
            if (leftPane != null) {
                var nodes = new ArrayList<>(leftPane.lookupAll(".displayNamesLabel"));
                for (int i = 0; i < nodes.size(); i++) {
                    String emailAddresses = nodes.get(i).getId();
                    System.out.println(emailAddresses);
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
                            /*else {

                            }*/
                        }
                    }
                }
            }
        });
    }
}
