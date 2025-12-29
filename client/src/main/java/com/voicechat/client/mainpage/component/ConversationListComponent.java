package com.voicechat.client.mainpage.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.login.component.AuthenticatePopupComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import com.voicechat.client.common.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationListComponent {

    private final MainPageService mainPageService = new MainPageService();

    private final AvatarComponent avatarComponent = new AvatarComponent();

    public void setConversationClicked(VBox leftPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
            Conversation conversation = null;
            var nodes = leftPane.lookupAll(".discussionBox");
            try {
                conversation = objectMapper.readValue(serverResponse.getBinaryPayload(), Conversation.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (conversation != null) {
                for (var node : nodes) {
                    StackPane stackPane = (StackPane) node;
                    VBox mainVbox = (VBox) stackPane.getChildren().getFirst();
                    String vBoxId = mainVbox.getId().replace("c", "");
                    if (conversation.getId() == Long.parseLong(vBoxId)) {
                        mainVbox.getStyleClass().add("conversationClicked");
                    }
                }
            }
        });
    }

    public VBox addLastMessageLabel(Message lastMessage) {
        VBox content = new VBox();
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);
        Label contentLabel = new Label();
        contentLabel.setText(lastMessage.getContent());
        contentLabel.getStyleClass().add("conversationLastMessageLabel");
        content.getChildren().add(contentLabel);
        return content;
    }

    public void setLastMessageOnSchedule(VBox leftPane, ServerResponse serverResponse) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Conversation conversation = objectMapper.readValue(serverResponse.getBinaryPayload(), Conversation.class);
        List<Message> messages = conversation.getMessages();
        Platform.runLater(() -> {
            if (!CollectionUtils.isEmpty(messages)) {
                Message lastMessage = conversation.getMessages().get(conversation.getMessages().size() - 1);
                VBox mainVbox = (VBox) leftPane.lookup("#c" + conversation.getId());
                Label contentLabel = (Label) mainVbox.lookup(".conversationLastMessageLabel");
                Label timeLabel = (Label) mainVbox.lookup(".dateDiscussionLabel");
                contentLabel.setText(lastMessage.getContent());
                timeLabel.setText(DateHandler.transformDate(lastMessage.getTime()));
            }
        });
    }

    public void setConversationList(MainPageController mainPageController, VBox leftPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
            List<Conversation> conversations = null;
            try {
                conversations = objectMapper.readValue(serverResponse.getBinaryPayload(),
                        new TypeReference<List<Conversation>>() {});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            AuthenticatePopupComponent.closePopup();
            User currentUser = UserSession.INSTANCE.getUser();
            currentUser.setConversation(conversations);
            for (Conversation conversation : conversations) {
                VBox mainVbox = new VBox();
                HBox hBox = new HBox();
                mainVbox.setId("c" + conversation.getId());
                VBox vBox = new VBox();
                VBox vbox2 = new VBox();
                VBox displayNames = new VBox();
                Label conversationName = new Label();
                List<String> displayNamesList = new ArrayList<>();
                List<String> emailAddressList = new ArrayList<>();
                int i = 0;
                for (User participant : conversation.getParticipants()) {
                    if (!StringUtils.equals(participant.getEmailAddress(), currentUser.getEmailAddress())) {
                        if (i == 0) {
                            try {
                                String correlationId = UUID.randomUUID().toString();
                                mainPageService.sendAvatarInfo(correlationId, participant);
                                vbox2 = new VBox();
                                StackPane stackAvatar = new StackPane();
                                stackAvatar.getStyleClass().add("stackAvatarConversationList");
                                ImageView imageView = Listener.getServerReader().getAvatarByCorrelationId(correlationId);
                                imageView.setFitWidth(40.);
                                imageView.setFitHeight(40.);
                                stackAvatar.getChildren().add(imageView);
                                vbox2.getChildren().add(stackAvatar);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        emailAddressList.add(participant.getEmailAddress());
                        displayNamesList.add(participant.getDisplayName());
                        i++;
                    }
                }
                Message lastMessage = conversation.getMessages().get(conversation.getMessages().size() - 1);
                displayNames.setId(String.join(",", emailAddressList));
                conversationName.setText(String.join(",", displayNamesList));
                conversationName.getStyleClass().add("conversationNameLabel");
                displayNames.getStyleClass().add("displayNamesLabel");
                displayNames.getChildren().add(conversationName);
                vBox.getChildren().addAll(displayNames);

                vBox.getChildren().add(addLastMessageLabel(lastMessage));

                vbox2.setAlignment(Pos.CENTER);
                vBox.setAlignment(Pos.CENTER);

                //mainVbox.getStyleClass().add("discussionBox");

                hBox.getChildren().add(vbox2);
                hBox.getChildren().add(vBox);

                HBox hBoxTime = new HBox();
                VBox vboxTime = new VBox();
                Label timeLabel = new Label();
                timeLabel.setText(DateHandler.transformDate(lastMessage.getTime()));
                timeLabel.getStyleClass().add("dateDiscussionLabel");
                hBoxTime.setAlignment(Pos.CENTER);
                hBoxTime.getStyleClass().add("dateDiscussion");
                vboxTime.getChildren().add(timeLabel);
                hBoxTime.getChildren().add(vboxTime);

                mainVbox.getChildren().addAll(hBoxTime, hBox);

                // Unread messages
                List<ReadStatus> readStatuses = conversation.getMessages().stream().flatMap(
                        message -> message.getReadStatuses().stream())
                        .toList();
                List<ReadStatus> unread = readStatuses.stream().filter(r ->
                                StringUtils.equals(r.getUser().getEmailAddress(), currentUser.getEmailAddress()))
                        .filter(r -> !r.isRead())
                        .toList();
                Integer unreadMessages = unread.size();


                StackPane contentStackPane = new StackPane();
                contentStackPane.getChildren().add(mainVbox);
                contentStackPane.getStyleClass().add("discussionBox");

                circleUnread(unreadMessages, contentStackPane);

                leftPane.getChildren().add(contentStackPane);
                mainPageController.goToConversation(contentStackPane);
            }
        });
    }

    public void circleUnread(Integer size, StackPane stackPane) {
        StackPane stackPaneCircle = new StackPane();
        Circle circle = new Circle(10);
        circle.setFill(Color.BLUE);
        HBox hBox = new HBox(10);
        hBox.setPadding(new Insets(25, 10, 10, 10));
        hBox.setAlignment(Pos.CENTER_RIGHT);

        if (size > 0) {
            Label numberLabel = new Label();
            numberLabel.getStyleClass().add("numberUnreadLabel");
            if (size > 9) {
                numberLabel.setText("9+");
            } else {
                numberLabel.setText(size.toString());
            }
            stackPaneCircle.getChildren().addAll(circle, numberLabel);
            hBox.getChildren().add(stackPaneCircle);
            stackPane.getChildren().add(hBox);
        }
    }
}
