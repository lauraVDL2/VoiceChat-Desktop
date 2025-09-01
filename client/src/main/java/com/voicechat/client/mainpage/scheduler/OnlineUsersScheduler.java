package com.voicechat.client.mainpage.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.mainpage.component.AvatarComponent;
import javafx.scene.layout.GridPane;
import org.shared.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnlineUsersScheduler {

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private List<ScheduledFuture<?>> onlineSchedules = new ArrayList<>();

    public void schedule(GridPane gridPane, OnlineFetch onlineFetch) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        onlineSchedules.add(scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchLoggedUsers(gridPane, onlineFetch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3, 60, TimeUnit.SECONDS));
    }

    public void fetchLoggedUsers(GridPane gridPane, OnlineFetch onlineFetch) throws IOException, InterruptedException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = new Message(MessageType.ONLINE_USERS_FETCH, "");
        PrintWriter serverOut = Listener.getServerOut();

        serverOut.println(objectMapper.writeValueAsString(message));

        ServerResponse serverResponse = Listener.getServerReader().getServerResponse();
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

    public List<ScheduledFuture<?>> getOnlineSchedules() {
        return onlineSchedules;
    }

    public void setOnlineSchedules(List<ScheduledFuture<?>> onlineSchedules) {
        this.onlineSchedules = onlineSchedules;
    }

    public void waitOnlineScheduleToBeDone() {
        this.onlineSchedules.forEach(s -> {
            try {
                s.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
