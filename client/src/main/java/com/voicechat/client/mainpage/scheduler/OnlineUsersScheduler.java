package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.mainpage.component.AvatarComponent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.shared.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnlineUsersScheduler {

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private Timeline onlineTimeline = null;

    public void schedule(GridPane gridPane, OnlineFetch onlineFetch) {
        if (onlineTimeline == null) {
            // Create a Timeline that triggers every 60 seconds
            onlineTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(60), event -> {
                        try {
                            String correlationId = UUID.randomUUID().toString();
                            fetchLoggedUsers(correlationId, gridPane, onlineFetch);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
            );
        }
        // Set it to repeat indefinitely
        onlineTimeline.setCycleCount(Timeline.INDEFINITE);
        onlineTimeline.play();
    }

    public void fetchLoggedUsers(String correlationId, GridPane gridPane, OnlineFetch onlineFetch) throws IOException, InterruptedException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = new Message(MessageType.ONLINE_USERS_FETCH, "");
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            serverOut.println(objectMapper.writeValueAsString(message));
        }

        var serverResponse = Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
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

    public void waitOnlineScheduleToBeDone() {
        if (onlineTimeline != null) {
            this.onlineTimeline.stop();
        }
    }

}
