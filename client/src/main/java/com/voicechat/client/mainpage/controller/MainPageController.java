package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.component.AvatarComponent;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.scheduler.OnlineUsersScheduler;
import com.voicechat.client.mainpage.service.MainPageService;
import com.voicechat.client.utils.DateHandler;
import javafx.application.Platform;
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
    @FXML
    private VBox rightSearchPane;

    private HeaderController headerController;

    private final MainPageService mainPageService = new MainPageService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final OnlineUsersScheduler onlineUsersScheduler = new OnlineUsersScheduler();

    private final ConversationComponent conversationComponent = new ConversationComponent();

    private final AvatarComponent avatarComponent = new AvatarComponent();

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
            Platform.runLater(() -> {
                double totalWidth = splitPane.getWidth();
                if (totalWidth > 0) {
                    double dividerPos = 350 / totalWidth;
                    splitPane.setDividerPositions(dividerPos);
                }
                leftPane.setMinWidth(200);
            });
            gridPaneFocus();
            getUserConversations();
            onlineUsersScheduler.schedule(gridMainPane);
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
                            conversationComponent.setConversationList(this, leftPane, serverResponse);
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
                headerController.clearSearchField();
            });
        }
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
                String targetUserEmailAddress = emailAddressField.getText();
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
                                    conversationComponent.setConversationComponents(mainPane, serverResponse);
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

    public void goToConversation(VBox mainVbox) {
        mainVbox.setOnMouseClicked(event -> {
            // Cast the event source to HBox
            Node source = (Node) event.getSource();
            if (source instanceof VBox) {
                VBox clickedVBox = (VBox) source;
                source.getStyleClass().add("conversationClicked");
                Conversation conversation = new Conversation();
                conversation.setId(Long.parseLong(clickedVBox.getId().replace("c", "")));
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
                                    conversationComponent.setMessagesComponents(this, rightSearchPane, mainPane, serverResponse);
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

    public void sendMessageToExistingConversation(ImageView imageView, Conversation conversation) {
        imageView.setOnMouseClicked((event -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    Conversation conversation1 = new Conversation();
                    conversation1.setId(conversation.getId());
                    Message message = new Message();
                    String content = ((TextField) mainPane.lookup("#sendMessage")).getText();
                    User sender = UserSession.INSTANCE.getUser();
                    message.setContent(content);
                    message.setSender(sender);
                    message.setTime(LocalDateTime.now());
                    conversation1.setMessages(List.of(message));
                    return mainPageService.sendMessage(conversation1);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MESSAGE_SENT) {
                            System.out.println("Message sent !");
                            try {
                                conversationComponent.addMessageComponents(mainPane, serverResponse);
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
        }));

    }
    
}
