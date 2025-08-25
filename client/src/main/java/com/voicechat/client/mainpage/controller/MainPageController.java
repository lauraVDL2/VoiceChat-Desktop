package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPageController {

    @FXML
    private VBox leftPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private GridPane gridMainPane;
    @FXML
    private BorderPane mainPane;

    private HeaderController headerController;

    private final MainPageService mainPageService = new MainPageService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/header.fxml"));
                Parent childNode = loader.load();
                headerController = loader.getController();
                headerController.setParentController(this);

                gridMainPane.getChildren().add(childNode);

            } catch (IOException e) {
                e.printStackTrace();
            }
            double totalWidth = splitPane.getWidth();
            if (totalWidth > 0) {
                double dividerPos = 350 / totalWidth;
                splitPane.setDividerPositions(dividerPos);
            }
            leftPane.setMinWidth(200);
            gridPaneFocus();
            getUserConversations();
        });
    }

    public void getUserConversations() {
        Platform.runLater(() -> {
            User user = UserSession.INSTANCE.getUser();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return mainPageService.displayUserConversations(user);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_DISPLAYED) {
                            System.out.println("Conversation displayed !" + serverResponse.getPayload());
                            setConversationList(serverResponse);
                        }
                    }
                    else {
                        System.out.println("ERROR");
                    }
                }
            }, executor);
        });
    }

    public void gridPaneFocus() {
        var children = gridMainPane.getChildren();
        for (var child : children) {
            child.setOnMouseClicked(mouseEvent -> {
                child.requestFocus();
                headerController.getSearchField().clear();
            });
        }
    }

    public VBox readTargetAvatar(VBox vBox) {
        try {
            DataInputStream dataInputStream = Listener.getDataInputStream();
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] imageBytes = new byte[size];
                dataInputStream.readFully(imageBytes);
                javafx.scene.image.Image image = new Image(new ByteArrayInputStream(imageBytes));
                ImageView avatar = new ImageView();
                avatar.setFitHeight(40);
                avatar.setFitWidth(40);
                Platform.runLater(() -> {
                            avatar.setImage(image);
                            vBox.getChildren().add(avatar);
                        }
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vBox;
    }
    
    public void sendMessage() {
        Platform.runLater(() -> {
            ImageView btn = (ImageView) mainPane.lookup("#sendButton");
            btn.setOnMouseClicked((event) -> {
                TextField sendMessage = (TextField) mainPane.lookup("#sendMessage");
                Conversation conversation = new Conversation();
                User currentUser = UserSession.INSTANCE.getUser();
                User targetUser = new User();
                targetUser.setDisplayName(((Label) mainPane.lookup("#displayNameLabelConv")).getText());
                Label emailAddressField = (Label) mainPane.lookup("#emailAddressLabelConv");
                String targetUserEmailAddress = emailAddressField.getText().replace("(", "").replace(")", "");
                targetUser.setEmailAddress(targetUserEmailAddress);
                conversation.setParticipants(Set.of(currentUser, targetUser));
                Message message = new Message();
                message.setContent(sendMessage.getText());
                message.setTime(LocalDateTime.now());

                ReadStatus readStatus = new ReadStatus(false, message, targetUser);
                ReadStatus currentUserReadStatus = new ReadStatus(true, message, currentUser);
                message.setReadStatuses(List.of(currentUserReadStatus, readStatus));
                message.setSender(currentUser);

                List<Message> currentUserMessages = currentUser.getMessages();
                currentUserMessages.add(message);

                currentUser.setMessages(currentUserMessages);

                conversation.setMessages(List.of(message));

                CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.createConversation(conversation);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync((serverResponse) -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_CREATED) {
                                System.out.println("conversation exists !");
                                try {
                                    setConversationComponents(serverResponse);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        else {
                            System.out.println("ERROR");
                        }
                    }
                }, executor);
            });
        });
    }

    public void setConversationList(ServerResponse serverResponse) {
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
                HBox hBox = new HBox();
                hBox.setId("c" + conversation.getId());
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
                                vbox2 = readTargetAvatar(vbox2);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                        }
                        emailAddressList.add(participant.getEmailAddress());
                        displayNamesList.add(participant.getDisplayName());
                        i++;
                    }
                }
                displayNames.setId(String.join(",", emailAddressList));
                conversationName.setText(String.join(",", displayNamesList));
                displayNames.getChildren().add(conversationName);
                vBox.getChildren().add(displayNames);

                Message lastMessage = conversation.getMessages().get(conversation.getMessages().size() - 1);
                VBox content = new VBox();
                Label contentLabel = new Label();
                contentLabel.setText(lastMessage.getContent());
                content.getChildren().add(contentLabel);
                vBox.getChildren().add(content);

                vbox2.setAlignment(Pos.CENTER);
                vBox.setAlignment(Pos.CENTER);

                hBox.getStyleClass().add("discussionBox");

                hBox.getChildren().add(vbox2);
                hBox.getChildren().add(vBox);
                leftPane.getChildren().add(hBox);
                goToConversation(hBox);
            }
        });
    }

    public void goToConversation(HBox hBox) {
        hBox.setOnMouseClicked(event -> {
            System.out.println("Go to conversation !");
            // Cast the event source to HBox
            Node source = (Node) event.getSource();
            if (source instanceof HBox) {
                HBox clickedHBox = (HBox) source;
                Conversation conversation = new Conversation();
                conversation.setId(Long.parseLong(clickedHBox.getId().replace("c", "")));
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.getConversation(conversation);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync((serverResponse) -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_GET) {
                                System.out.println("conversation found !");
                                try {
                                    setMessagesComponents(serverResponse);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            System.out.println("ERROR");
                        }
                    }
                }, executor);
            }
        });
    }

    public void setMessagesComponents(ServerResponse response) throws JsonProcessingException {
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

            Label introLabel = new Label();
            introLabel.setText("Your conversation with : #");
            targetUserInfo.getChildren().add(introLabel);

            for (User participant : conversation.getParticipants()) {
                if (!StringUtils.equals(currentUser.getEmailAddress(), participant.getEmailAddress())) {
                    Label displayNameLabel = new Label();
                    displayNameLabel.setText(participant.getDisplayName());
                    targetUserInfo.getChildren().add(displayNameLabel);
                    Label emailAddressLabel = new Label();
                    emailAddressLabel.setText("(" + participant.getEmailAddress() + ")");
                    emailAddressLabel.setId("e" + participant.getId());
                    targetUserInfo.setAlignment(Pos.CENTER);
                    targetUserInfo.getChildren().add(emailAddressLabel);
                }
            }

            HBox.setMargin(targetUserInfo, new Insets(0, 0, 0, 30));
            hBox.getChildren().add(targetUserInfo);

            HBox optionsBox = new HBox();
            HBox.setHgrow(optionsBox, Priority.ALWAYS);
            optionsBox.setAlignment(Pos.CENTER_RIGHT);
            TextField searchMessage = new TextField();
            optionsBox.getChildren().add(searchMessage);
            hBox.getChildren().add(optionsBox);
            mainPane.setTop(hBox);

            //MIDDLE (messages)
            for (Message message : conversation.getMessages()) {
                HBox hBoxMessage = new HBox();
                VBox vBoxSender = new VBox();
                Label labelSender = new Label();
                LocalDateTime date = message.getTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDate = date.format(formatter);
                labelSender.setText("by " + message.getSender().getDisplayName() + " at " + formattedDate);
                vBoxSender.getChildren().add(labelSender);
                Label conversationMessage = new Label();
                conversationMessage.setText(message.getContent());
                vBoxSender.getChildren().add(conversationMessage);
                hBoxMessage.getChildren().add(vBoxSender);
                mainPane.setCenter(hBoxMessage);
            }

            //Bottom
            HBox hBox1 = new HBox();
            hBox1.setId("sendBox");
            TextField messageField = new TextField();
            messageField.setId("sendMessage");
            ImageView imageView = new ImageView();
            imageView.setFitHeight(40);
            imageView.setFitHeight(40);
            Image image = new Image(VoiceChatApplication.class.getResourceAsStream("images/send-button.png"));
            imageView.setId("sendButton");
            imageView.setImage(image);
            hBox1.getChildren().add(messageField);
            hBox1.getChildren().add(imageView);
            mainPane.setBottom(hBox1);
        });
    }

    public void setConversationComponents(ServerResponse serverResponse) throws JsonProcessingException {
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
    
}
