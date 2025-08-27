package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.voicechat.client.Listener;
import com.voicechat.client.mainpage.component.AvatarComponent;
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

    private final AvatarComponent avatarComponent = new AvatarComponent();

    public void schedule(GridPane gridPane, OnlineFetch onlineFetch) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchLoggedUsers(gridPane, onlineFetch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void fetchLoggedUsers(GridPane gridPane, OnlineFetch onlineFetch) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = new Message(MessageType.ONLINE_USERS_FETCH, "");
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        String serverInLine = Listener.getServerIn().readLine();

        if (StringUtils.isNotBlank(serverInLine)) {
            ServerResponse serverResponse = objectMapper.readValue(serverInLine, ServerResponse.class);
            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.ONLINE_USERS_FETCHED) {
                if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    switch (onlineFetch) {
                        case CONVERSATION_LIST:
                            avatarComponent.modifyUsersOnlineConversationList(gridPane, serverResponse);
                            break;
                        case MESSAGES:
                            avatarComponent.modifyUsersOnlineMessage(gridPane, serverResponse);
                            break;
                    }
                }
            }
        }
    }

}
