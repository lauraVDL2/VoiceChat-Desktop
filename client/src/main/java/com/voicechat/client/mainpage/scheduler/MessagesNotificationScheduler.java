package com.voicechat.client.mainpage.scheduler;

import com.voicechat.client.Listener;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.commons.collections4.CollectionUtils;
import org.shared.*;
import org.shared.entity.Conversation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class MessagesNotificationScheduler {

    private Timeline messageNotificationSchedule = null;

    public void schedule(String emailAddress, MainPageController mainPageController, BorderPane borderPane,
                         VBox leftPane, ConversationComponent conversationComponent,
                         ConversationListComponent conversationListComponent, Conversation conversation,
                         GridPane gridMainPane, OnlineUsersScheduler onlineUsersScheduler) {

        // Create a Timeline that triggers every 10 seconds
        if (messageNotificationSchedule == null) {
            messageNotificationSchedule = new Timeline(
                    new KeyFrame(Duration.seconds(10), event -> {
                        // Your scheduled task
                        try {
                            onlineUsersScheduler.waitOnlineScheduleToBeDone();
                            onlineUsersScheduler.fetchLoggedUsers(UUID.randomUUID().toString(), gridMainPane, OnlineFetch.MESSAGES);

                            List<ServerResponse> serverResponses = Listener.getServerReader()
                                    .getServerResponseBySpecificField("notif-" + emailAddress);
                            if (CollectionUtils.isNotEmpty(serverResponses)) {
                                for (ServerResponse serverResponse : serverResponses) {
                                    if (serverResponse != null) {
                                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.NEW_MESSAGE_NOTIFIED
                                                && serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                                            Platform.runLater(() -> {
                                                try {
                                                    conversationComponent.addMessagesReceivedComponents(
                                                            mainPageController, serverResponse, borderPane, conversation);
                                                    conversationListComponent.setLastMessageOnSchedule(leftPane, serverResponse);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
            );
        }

        // Set the cycle count to indefinite for continuous running
        messageNotificationSchedule.setCycleCount(Timeline.INDEFINITE);
        messageNotificationSchedule.play();
    }

    public void waitMessageScheduleToBeDone() {
        if (messageNotificationSchedule != null) {
            messageNotificationSchedule.stop();
        }
    }

}
