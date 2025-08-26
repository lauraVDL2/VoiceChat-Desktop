package com.voicechat.client.mainpage.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationComponent {

    private final MainPageService mainPageService = new MainPageService();

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void setConversationComponents(BorderPane mainPane, ServerResponse serverResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Conversation conversation = objectMapper.readValue(serverResponse.getPayload(), Conversation.class);
        UserSession.INSTANCE.getUser().getConversation().add(conversation);
        HBox hBox = new HBox();
        for (User user : conversation.getParticipants()) {
            if (StringUtils.equals(user.getEmailAddress(), UserSession.INSTANCE.getUser().getEmailAddress())) {
                VBox vBox1 = new VBox();
                Label labelName = new Label();
                labelName.setText(user.getDisplayName());
                Label labelTime = new Label();
                Message conversationMessage = conversation.getMessages().stream().findFirst().orElse(null);
                Message message = user.getMessages().stream()
                        .filter(userMessage -> userMessage.getId() == conversationMessage.getId())
                        .findFirst().orElse(null);
                labelTime.setText(" - " + message.getTime());
                vBox1.getChildren().add(labelName);
                vBox1.getChildren().add(labelTime);
                hBox.getChildren().add(vBox1);

                VBox vBox2 = new VBox();
                Label messageContent = new Label();
                messageContent.setText(message.getContent());
                vBox2.getChildren().add(messageContent);
                hBox.getChildren().add(vBox2);
            }
        }
        mainPane.setCenter(hBox);
    }

    public void addMessageComponents(BorderPane mainPane, ServerResponse serverResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = objectMapper.readValue(serverResponse.getPayload(), Message.class);
        User currentUser = UserSession.INSTANCE.getUser();

        Platform.runLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    mainPageService.sendAvatarInfo(message.getSender());
                    return avatarComponent.readTargetAvatar(new VBox());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return new VBox();
                }
            }).thenAcceptAsync(avatarBox -> {
                // Update UI on JavaFX Application Thread
                avatarBox.setAlignment(Pos.CENTER);
                HBox hBoxAvatar = new HBox();
                hBoxAvatar.getChildren().add(avatarBox);

                VBox messageContentBox = (VBox) mainPane.lookup("#messageContentBox");
                VBox vbox = addMessageBox(hBoxAvatar, messageContentBox, message, currentUser);
                mainPane.setCenter(vbox);
            });
        });
    }

    public VBox addMessageBox(HBox hBoxAvatar, VBox messageContentBox, Message message, User currentUser) {
        HBox hBoxMessage = new HBox();
        hBoxMessage.getStyleClass().add("boxMessage");
        Platform.runLater(() -> {
            VBox vBoxSender = new VBox();
            vBoxSender.setAlignment(Pos.CENTER_LEFT);
            HBox hBoxSender = new HBox();

            Label labelSender = new Label();
            LocalDateTime date = message.getTime();

            if (StringUtils.equals(message.getSender().getEmailAddress(), currentUser.getEmailAddress())) {
                labelSender.setText("You");
                vBoxSender.getStyleClass().add("myMessageBox");
            } else {
                labelSender.setText(message.getSender().getDisplayName());
                vBoxSender.getStyleClass().add("senderMessageBox");
            }

            labelSender.getStyleClass().add("labelSender");
            hBoxSender.getChildren().add(labelSender);

            Label space = new Label(" ");
            hBoxSender.getChildren().add(space);

            Label labelTime = new Label(DateHandler.transformDate(date));
            labelTime.getStyleClass().add("labelText");
            hBoxSender.getChildren().add(labelTime);

            vBoxSender.getChildren().add(hBoxSender);

            Label conversationMessage = new Label();
            conversationMessage.getStyleClass().add("conversationMessageLabel");
            conversationMessage.setText(message.getContent());
            vBoxSender.getChildren().add(conversationMessage);

            if (!StringUtils.equals(message.getSender().getEmailAddress(), currentUser.getEmailAddress())) {
                hBoxMessage.getChildren().addAll(hBoxAvatar, vBoxSender);
            } else {
                hBoxMessage.getChildren().addAll(vBoxSender, hBoxAvatar);
                hBoxMessage.setAlignment(Pos.BASELINE_RIGHT);
            }
            Region region = new Region();
            region.setMinHeight(10);
            messageContentBox.getChildren().addAll(region, hBoxMessage);
        });
        return messageContentBox;
    }

    public void setMessagesComponents(MainPageController mainPageController, VBox rightSearchPane, BorderPane mainPane, ServerResponse response) throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Conversation conversation = mapper.readValue(response.getPayload(), Conversation.class);
        Platform.runLater(() -> {
            User currentUser = UserSession.INSTANCE.getUser();

            HBox hBox = new HBox();
            hBox.getStyleClass().add("topConversationBox");
            hBox.setPrefHeight(40.);
            hBox.setMaxHeight(40.);
            hBox.setMinHeight(40.);
            hBox.setAlignment(Pos.CENTER);

            HBox targetUserInfo = new HBox();
            targetUserInfo.getStyleClass().add("topConversationLabels");

            int i = 0;
            for (User participant : conversation.getParticipants()) {
                if (!StringUtils.equals(currentUser.getEmailAddress(), participant.getEmailAddress())) {
                    if (i == 0) {
                        try {
                            mainPageService.sendAvatarInfo(participant);
                            ImageView avatarView = avatarComponent.readTargetAvatar();
                            targetUserInfo.getChildren().add(avatarView);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                    i++;
                    Label displayNameLabel = new Label();
                    displayNameLabel.setText(participant.getDisplayName());
                    targetUserInfo.getChildren().add(displayNameLabel);

                    Label emailAddressLabel = new Label();
                    emailAddressLabel.setText(participant.getEmailAddress());
                    emailAddressLabel.setId("e" + participant.getId());
                    emailAddressLabel.setManaged(false);
                    emailAddressLabel.setVisible(false);
                    targetUserInfo.setAlignment(Pos.CENTER);
                    targetUserInfo.getChildren().add(emailAddressLabel);
                }
            }

            HBox.setMargin(targetUserInfo, new Insets(0, 0, 0, 30));
            hBox.getChildren().add(targetUserInfo);

            HBox optionsBox = new HBox();
            HBox.setHgrow(optionsBox, Priority.ALWAYS);
            optionsBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.getChildren().add(optionsBox);
            mainPane.setTop(hBox);

            // MIDDLE (messages)
            VBox messageContentBox = new VBox();
            messageContentBox.setId("messageContentBox");
            messageContentBox.setPadding(new Insets(10, 10, 10, 10));
            for (Message message : conversation.getMessages()) {
                // Asynchronous avatar loading
                Platform.runLater(() -> {
                    try {
                        HBox hBoxAvatar = new HBox();
                        mainPageService.sendAvatarInfo(message.getSender());
                        VBox avatarBox = avatarComponent.readTargetAvatar(new VBox());
                        avatarBox.setAlignment(Pos.CENTER);
                        hBoxAvatar.getChildren().add(avatarBox);
                        // Assuming addMessageBox modifies in place or returns void
                        addMessageBox(hBoxAvatar, messageContentBox, message, currentUser);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }

            mainPane.setCenter(messageContentBox);

            // Bottom send box
            HBox hBox1 = new HBox();
            hBox1.setId("sendBox");
            hBox1.setAlignment(Pos.CENTER);

            TextField messageField = new TextField();
            messageField.setId("sendMessage");
            messageField.getStyleClass().add("sendMessageField");

            ImageView imageView = new ImageView();
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            Image image = new Image(VoiceChatApplication.class.getResourceAsStream("images/send-button.png"));
            imageView.setId("sendButton");
            imageView.setImage(image);

            mainPageController.sendMessageToExistingConversation(imageView, conversation);

            Region spacer = new Region();
            spacer.setPrefWidth(10);
            hBox1.getChildren().addAll(messageField, spacer, imageView);
            mainPane.setBottom(hBox1);

            mainPane.setPadding(new Insets(0, 0, 10, 0));

            // RIGHT pane
            rightSearchPane.getChildren().clear();
            Region region = new Region();
            region.setMinHeight(20);
            TextField searchMessages = new TextField();
            searchMessages.setPromptText("Search for messages...");
            searchMessages.getStyleClass().add("searchMessages");
            rightSearchPane.setAlignment(Pos.TOP_CENTER);
            rightSearchPane.getChildren().addAll(region, searchMessages);
        });
    }

    public void setConversationList(MainPageController mainPageController, VBox leftPane, ServerResponse serverResponse) {
        Platform.runLater(() -> {
            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
            List<Conversation> conversations = null;
            try {
                conversations = objectMapper.readValue(serverResponse.getPayload(),
                        new TypeReference<List<Conversation>>() {});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
                                mainPageService.sendAvatarInfo(participant);
                                vbox2 = new VBox();
                                StackPane stackAvatar = new StackPane();
                                stackAvatar.getStyleClass().add("stackAvatarConversationList");
                                ImageView imageView = avatarComponent.readTargetAvatar();
                                stackAvatar.getChildren().add(imageView);
                                vbox2.getChildren().add(stackAvatar);
                            } catch (JsonProcessingException e) {
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

                VBox content = new VBox();
                Label contentLabel = new Label();
                contentLabel.setText(lastMessage.getContent());
                contentLabel.getStyleClass().add("conversationLastMessageLabel");
                content.getChildren().add(contentLabel);
                vBox.getChildren().add(content);

                vbox2.setAlignment(Pos.CENTER);
                vBox.setAlignment(Pos.CENTER);

                mainVbox.getStyleClass().add("discussionBox");

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

                leftPane.getChildren().add(mainVbox);
                mainPageController.goToConversation(mainVbox);
            }
        });
    }

}
