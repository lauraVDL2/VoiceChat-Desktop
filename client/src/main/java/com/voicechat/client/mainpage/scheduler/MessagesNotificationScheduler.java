package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.ServerReader;
import com.voicechat.client.Listener;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;
import org.shared.entity.Conversation;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class MessagesNotificationScheduler {

    private ScheduledFuture<?> messageNotificationSchedule = null;

    public void schedule(String emailAddress, MainPageController mainPageController, BorderPane borderPane, VBox leftPane, ConversationComponent conversationComponent,
                         ConversationListComponent conversationListComponent, Conversation conversation,
                         GridPane gridMainPane, OnlineUsersScheduler onlineUsersScheduler) {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        messageNotificationSchedule = scheduler.scheduleAtFixedRate(() -> {
                try {
                    onlineUsersScheduler.waitOnlineScheduleToBeDone();
                    onlineUsersScheduler.fetchLoggedUsers(UUID.randomUUID().toString(), gridMainPane, OnlineFetch.MESSAGES);
                    List<ServerResponse> serverResponses = Listener.getServerReader().getServerResponseByEmail("notif-" + emailAddress);
                    if (CollectionUtils.isNotEmpty(serverResponses)) {
                        for (ServerResponse serverResponse : serverResponses) {
                            if (serverResponse != null) {
                                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.NEW_MESSAGE_NOTIFIED
                                        && serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                                    Platform.runLater(() -> {
                                        try {
                                            conversationComponent.addMessagesReceivedComponents(mainPageController, serverResponse, borderPane, conversation);
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
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void waitMessageScheduleToBeDone() {
        /*try {
            messageNotificationSchedule.cancel(true);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

}
