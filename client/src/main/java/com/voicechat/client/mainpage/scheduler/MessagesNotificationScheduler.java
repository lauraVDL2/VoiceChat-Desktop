package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.mainpage.component.ConversationComponent;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.StringUtils;
import org.shared.*;
import org.shared.entity.Conversation;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessagesNotificationScheduler {

    public void schedule(BorderPane borderPane, ConversationComponent conversationComponent, Conversation conversation) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchLastMessagesInConversation(borderPane, conversationComponent, conversation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void fetchLastMessagesInConversation(BorderPane borderPane, ConversationComponent conversationComponent,
                                                Conversation conversation) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String serverInLine = Listener.getServerIn().readLine();

        if (StringUtils.isNotBlank(serverInLine)) {
            ServerResponse serverResponse = objectMapper.readValue(serverInLine, ServerResponse.class);
            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.NEW_MESSAGE_NOTIFIED) {
                if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    conversationComponent.addMessagesReceivedComponents(serverResponse, borderPane, conversation);
                }
            }
        }
    }
}
