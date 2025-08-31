package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.ServerReader;
import com.voicechat.client.Listener;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;
import org.shared.entity.Conversation;

import java.io.IOException;
import java.util.concurrent.*;

public class MessagesNotificationScheduler {

    public void schedule(BorderPane borderPane, VBox leftPane, ConversationComponent conversationComponent,
                         ConversationListComponent conversationListComponent, Conversation conversation,
                         GridPane gridMainPane, OnlineUsersScheduler onlineUsersScheduler) {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
                try {
                    ServerResponse serverResponse = Listener.getServerReader().getServerResponse();
                    System.out.println(serverResponse.getServerResponseMessage());
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.NEW_MESSAGE_NOTIFIED
                            && serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        Platform.runLater(() -> {
                            try {
                                conversationComponent.addMessagesReceivedComponents(serverResponse, borderPane, conversation);
                                conversationListComponent.setLastMessageOnSchedule(leftPane, serverResponse);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            /*try {
                onlineUsersScheduler.fetchLoggedUsers(gridMainPane, OnlineFetch.MESSAGES);
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }, 5, 5, TimeUnit.SECONDS);
    }

}
