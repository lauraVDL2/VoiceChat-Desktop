package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.component.*;
import com.voicechat.client.mainpage.scheduler.MessagesNotificationScheduler;
import com.voicechat.client.mainpage.scheduler.OnlineFetch;
import com.voicechat.client.mainpage.scheduler.OnlineUsersScheduler;
import com.voicechat.client.mainpage.service.HeaderService;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.apache.commons.collections4.CollectionUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.*;
import org.shared.pojo.Page;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    private final HeaderService headerService = new HeaderService();

    private final ConversationCreationComponent conversationCreationComponent = new ConversationCreationComponent();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final OnlineUsersScheduler onlineUsersScheduler = new OnlineUsersScheduler();

    private final MessagesNotificationScheduler messagesNotificationScheduler = new MessagesNotificationScheduler();

    private final ConversationComponent conversationComponent = new ConversationComponent();

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private final ConversationListComponent conversationListComponent = new ConversationListComponent();

    private final ConversationMessageSearchComponent conversationMessageSearchComponent = new ConversationMessageSearchComponent();

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

    public void createConversationBoxes(ImageView plusImageView) {
        conversationCreationComponent.createConversationBoxes(this, plusImageView, mainPane);
    }

    public void searchUsers(HBox hBox, VBox vBox, TextField searchField) {
        String searchDisplayName = searchField.getText().trim();

        // Avoid empty searches
        if (searchDisplayName.isEmpty()) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return headerService.searchUser(searchDisplayName);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executor).thenAcceptAsync((serverResponse) -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_SEARCHED) {
                        try {
                            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                            List<User> users = objectMapper.readValue(serverResponse.getBinaryPayload(),
                                    new TypeReference<List<User>>() {
                                    });
                            conversationCreationComponent.setSearchUsersContent(this, vBox, hBox, users);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, executor);
    }

    public void goToMessage(HBox hBox, Conversation conversation, Message message) {
        hBox.setOnMouseClicked(event -> {
            CompletableFuture.supplyAsync(() -> {
                        try {
                            conversation.setMessages(List.of(message));
                            return mainPageService.goToMessageInConversation(conversation);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }, executor).thenAcceptAsync(serverResponse -> {
                        if (serverResponse != null) {
                            if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                                if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MESSAGE_CONVERSATION_WENT) {
                                    try {
                                        Page page = serverResponse.getPage();
                                        conversationComponent.setPage(page);
                                        Conversation conversation1 = JsonMapper.getJsonMapper()
                                                .readValue(serverResponse.getBinaryPayload(), Conversation.class);
                                        Platform.runLater(() -> {
                                            mainPane.setCenter(
                                                    conversationComponent.addConversationMessagesScrollPane(
                                                            this, conversation1, gridMainPane, UserSession.INSTANCE.getUser(),
                                                            page, (ScrollPane) gridMainPane.lookup("#conversationScrollPane")
                                                    )
                                            );
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
        });
    }

    public void loadMoreConversationMessages(Conversation conversation, Page page, ScrollPane scrollPane, Runnable runnable) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.scrollMessages(conversation, page);
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
                                        mainPane.setCenter(conversationComponent.addConversationMessagesScrollPane(this, conversation1,
                                                gridMainPane, UserSession.INSTANCE.getUser(), page, scrollPane));
                                        // Execute the runnable after UI update
                                        if (runnable != null) {
                                            runnable.run();
                                        }
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

    public void scrollConversationMessages(Conversation conversation, Page page) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return mainPageService.scrollMessages(conversation, page);
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
                                        mainPane.setCenter(conversationComponent.addConversationMessagesScrollPane(this, conversation1, gridMainPane, UserSession.INSTANCE.getUser(), page,
                                                (ScrollPane) gridMainPane.lookup("#conversationScrollPane")));
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
                            conversationListComponent.setConversationList(this, mainPane, leftPane, serverResponse);
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
    
    public void sendMessage(Conversation conversation) {
        Platform.runLater(() -> {
//            ImageView btn = (ImageView) mainPane.lookup("#sendButton");
//            btn.setOnMouseClicked((event) -> {
//                TextArea sendMessage = (TextArea) mainPane.lookup("#sendMessage");
//                Conversation conversation = new Conversation();
//                User currentUser = UserSession.INSTANCE.getUser();
//                User targetUser = new User();
//                targetUser.setDisplayName(((Label) mainPane.lookup("#displayNameLabelConv")).getText());
//                Label emailAddressField = (Label) mainPane.lookup("#emailAddressLabelConv");
//                String targetUserEmailAddress = emailAddressField.getText();
//                targetUser.setEmailAddress(targetUserEmailAddress);
//                conversation.setParticipants(Set.of(currentUser, targetUser));
//                Message message = new Message();
//                message.setContent(sendMessage.getText());
//                message.setTime(LocalDateTime.now());
//
//                ReadStatus readStatus = new ReadStatus(false, message, targetUser);
//                ReadStatus currentUserReadStatus = new ReadStatus(true, message, currentUser);
//                message.setReadStatuses(List.of(currentUserReadStatus, readStatus));
//                message.setSender(currentUser);
//
//                List<Message> currentUserMessages = currentUser.getMessages();
//                currentUserMessages.add(message);
//
//                currentUser.setMessages(currentUserMessages);
//
//                conversation.setMessages(List.of(message));

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
//        });
    }

    public void getOrCreateConversationIfExists(Set<User> users, Message message) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return mainPageService.getConversationByUsers(users);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executor).thenAcceptAsync((serverResponse) -> {
            if (serverResponse != null) {
                if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_GET_BY_PARTICIPANTS) {
                        System.out.println("conversation exists !");
                        Platform.runLater(() -> {
                            try {
                                mainPane.getChildren().clear();
                                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                                Conversation conversation = objectMapper.readValue(serverResponse.getBinaryPayload(), Conversation.class);
                                conversationComponent.setMessagesComponents(this, gridMainPane, rightSearchPane, mainPane,
                                        serverResponse, conversationListComponent, messagesNotificationScheduler);
                                //conversationComponent.setConversationComponents(mainPane, serverResponse);
                                sendMessageToExistingConversation(conversation, message);
                                mainPane.requestLayout();
                                System.out.println("VA ICI");
                                if (conversation != null) {
                                    System.out.println(conversation.getId());
                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                else if (serverResponse.getServerResponseStatus() == ServerResponseStatus.INFO) {
                    if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_GET_BY_PARTICIPANTS) {
                        Platform.runLater(() -> {
                            mainPane.getChildren().clear();
                            createConversation(users, message);
                        });
                    }
                }
            }
        }, executor);
    }

    public void createConversation(Set<User> users, Message message) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Conversation conversation = new Conversation();
                conversation.setParticipants(users);
                conversation.setMessages(List.of(message));
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
                            conversationComponent.setMessagesComponents(this, gridMainPane, rightSearchPane, mainPane,
                                    serverResponse, conversationListComponent, messagesNotificationScheduler);
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

    public void searchMessageInConversation(VBox rightSearchPane, TextField searchMessage, Conversation conversation) {
        searchMessage.setOnAction(event -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String content = searchMessage.getText();
                    Message message = new Message();
                    message.setContent(content);
                    conversation.setMessages(List.of(message));
                    return mainPageService.searchMessageInConversation(conversation);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }, executor).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.MESSAGE_CONVERSATION_SEARCHED) {
                            try {
                                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                                List<Message> messages = objectMapper.readValue(serverResponse.getBinaryPayload(),
                                        new TypeReference<List<Message>>() {});
                                if (CollectionUtils.isNotEmpty(messages)) {
                                    conversationMessageSearchComponent.setMessagesSearched(rightSearchPane, messages, conversation, this);
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        });
    }

    public void sendMessageToExistingConversation(Conversation conversation, Message message) {
        //imageView.setOnMouseClicked((event -> {
        if (conversation == null || message == null) {
            return;
        }

        String correlationId = UUID.randomUUID().toString();

        Conversation outgoing = new Conversation();
        outgoing.setId(conversation.getId());
        outgoing.setMessages(List.of(message));

        CompletableFuture.supplyAsync(() -> {
            try {
                onlineUsersScheduler.waitOnlineScheduleToBeDone();
                messagesNotificationScheduler.waitMessageScheduleToBeDone();
                return mainPageService.sendMessage(correlationId, outgoing);
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
        //}));
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
