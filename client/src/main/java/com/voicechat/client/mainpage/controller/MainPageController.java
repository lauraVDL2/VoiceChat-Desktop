package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.voicechat.client.ServerReader;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.component.AvatarComponent;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.scheduler.MessagesNotificationScheduler;
import com.voicechat.client.mainpage.scheduler.OnlineFetch;
import com.voicechat.client.mainpage.scheduler.OnlineUsersScheduler;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.shared.JsonMapper;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

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

    private final MessagesNotificationScheduler messagesNotificationScheduler = new MessagesNotificationScheduler();

    private final ConversationComponent conversationComponent = new ConversationComponent();

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private final ConversationListComponent conversationListComponent = new ConversationListComponent();

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
            Platform.runLater(() -> {
                onlineUsersScheduler.schedule(gridMainPane, OnlineFetch.CONVERSATION_LIST);
            });
        });
    }

    public void scrollConversationMessages(Conversation conversation, int offset) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.scrollMessages(conversation, offset);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync(serverResponse -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_SCROLLED) {
                                try {
                                    Conversation conversation1 = JsonMapper.getJsonMapper().readValue(serverResponse.getBinaryPayload(), Conversation.class);
                                    Platform.runLater(() -> {
                                        mainPane.setCenter(conversationComponent.addConversationMessagesScrollPane(this, conversation1, gridMainPane, UserSession.INSTANCE.getUser()));
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            System.err.println("ERROR: Failed to scroll messages. Status: " + serverResponse.getServerResponseStatus());
                        }
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Exception while scrolling messages:");
                    ex.printStackTrace();
                    return null;
                });
    }

    public void getUserConversations() {
        Platform.runLater(() -> {
            User user = UserSession.INSTANCE.getUser();
            CompletableFuture.supplyAsync(() -> {
                try {
                    String correlationId = UUID.randomUUID().toString();
                    mainPageService.displayUserConversations(user, correlationId);
                    return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_DISPLAYED) {
                            conversationListComponent.setConversationList(this, leftPane, serverResponse);
                        }
                    }
                    else {
                        System.out.println("ERROR");
                    }
                }
            });
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
                TextArea sendMessage = (TextArea) mainPane.lookup("#sendMessage");
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
                    } catch (Exception e) {
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
                                } catch (IOException e) {
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

    public void goToConversation(StackPane contentStackPane) {
        contentStackPane.setOnMouseClicked(event -> {
            // Cast the event source to HBox
            Node source = (Node) event.getSource();
            if (source instanceof StackPane) {
                StackPane clickedStackPane = (StackPane) source;
                VBox clickedVBox = (VBox) clickedStackPane.getChildren().getFirst();
                source.getStyleClass().add("conversationClicked");
                Conversation conversation = new Conversation();
                conversation.setId(Long.parseLong(clickedVBox.getId().replace("c", "")));
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.getConversation(conversation);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync((serverResponse) -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_GET) {
                                System.out.println("conversation found !");
                                try {
                                    conversationComponent.setMessagesComponents(this, gridMainPane, rightSearchPane, mainPane,
                                            serverResponse, conversationListComponent, messagesNotificationScheduler);

                                } catch (IOException e) {
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
                    String content = ((TextArea) mainPane.lookup("#sendMessage")).getText();
                    User sender = UserSession.INSTANCE.getUser();
                    message.setContent(content);
                    message.setSender(sender);
                    message.setTime(LocalDateTime.now());
                    conversation1.setMessages(List.of(message));
                    onlineUsersScheduler.waitOnlineScheduleToBeDone();
                    messagesNotificationScheduler.waitMessageScheduleToBeDone();
                    String correlationId = UUID.randomUUID().toString();
                    return mainPageService.sendMessage(correlationId, conversation1);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MESSAGE_SENT) {
                                try {
                                    System.out.println("Message sent !");
                                    conversationComponent.addMessageComponents(this, mainPane, serverResponse, conversation);
                                    messagesNotificationScheduler.schedule("toto1.toto@yahoo.fr", this, mainPane, leftPane, conversationComponent,
                                            conversationListComponent, conversation, gridMainPane, onlineUsersScheduler);
                                    onlineUsersScheduler.schedule(gridMainPane, OnlineFetch.CONVERSATION_LIST);
                                } catch (IOException e) {
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

    public BorderPane getMainPane() {
        return mainPane;
    }

    public GridPane getGridMainPane() {
        return gridMainPane;
    }

    public VBox getRightSearchPane() {
        return rightSearchPane;
    }

    public VBox getLeftPane() {
        return leftPane;
    }

    public MessagesNotificationScheduler getMessagesNotificationScheduler() {
        return messagesNotificationScheduler;
    }
    
}
