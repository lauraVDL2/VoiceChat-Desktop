package com.voicechat.client.mainpage.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.common.utils.AnimationUtil;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.scheduler.MessagesNotificationScheduler;
import com.voicechat.client.mainpage.scheduler.OnlineUsersScheduler;
import com.voicechat.client.mainpage.service.MainPageService;
import com.voicechat.client.common.utils.DateHandler;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;
import org.shared.pojo.Page;
import org.shared.pojo.ScrollDirection;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ConversationComponent {

    private final MainPageService mainPageService = new MainPageService();

    private final OnlineUsersScheduler onlineUsersScheduler = new OnlineUsersScheduler();

    private final ConversationMessageSearchComponent conversationMessageSearchComponent = new ConversationMessageSearchComponent();

    private Page page = new Page();

    private double vvalue = 1.;

    private ScrollPane scrollPane;

    public void setConversationComponents(BorderPane mainPane, ServerResponse serverResponse) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Conversation conversation = objectMapper.readValue(serverResponse.getBinaryPayload(), Conversation.class);
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
                        .filter(userMessage -> Objects.equals(userMessage.getId(), conversationMessage.getId()))
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

    public void addMessageComponents(MainPageController mainPageController, BorderPane mainPane, ServerResponse serverResponse, Conversation conversation) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = objectMapper.readValue(serverResponse.getBinaryPayload(), Message.class);
        User currentUser = UserSession.INSTANCE.getUser();

        Platform.runLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String correlationId = UUID.randomUUID().toString();
                    mainPageService.sendAvatarInfo(correlationId, message.getSender());
                    return Listener.getServerReader().getAvatarByCorrelationId(correlationId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ImageView();
                }
            }).thenAcceptAsync(imageView -> {
                VBox avatarBox = new VBox();
                // Update UI on JavaFX Application Thread
                imageView.setFitHeight(40.);
                imageView.setFitWidth(40.);
                avatarBox.setAlignment(Pos.CENTER);
                HBox hBoxAvatar = new HBox();
                avatarBox.getChildren().add(imageView);
                hBoxAvatar.getChildren().add(avatarBox);

                VBox messageContentBox = (VBox) mainPane.lookup("#messageContentBox");
                VBox vbox = addMessageBox(hBoxAvatar, messageContentBox, message, currentUser);
                ScrollPane scrollPane = addMessagesScrollPane(mainPageController, vbox, conversation, page);
                mainPane.setCenter(scrollPane);
            });
        });
    }

    public void addMessagesReceivedComponents(MainPageController mainPageController, ServerResponse serverResponse, BorderPane mainPane, Conversation initialConversation)
            throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Conversation conversation = objectMapper.readValue(serverResponse.getBinaryPayload(), Conversation.class);
        User currentUser = UserSession.INSTANCE.getUser();



        if (conversation.getId() == initialConversation.getId()) {
            if (!CollectionUtils.isEmpty(conversation.getMessages())) {
                Message lastMessage = conversation.getMessages().get(0);
                if (!StringUtils.equals(lastMessage.getSender().getEmailAddress(), currentUser.getEmailAddress())) {
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return Listener.getServerReader().getAvatarByCorrelationId(serverResponse.getCorrelationId());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return new ImageView();
                            }
                        }).thenAcceptAsync(avatarImage -> {
                            avatarImage.setFitHeight(40.);
                            avatarImage.setFitWidth(40.);
                            VBox avatarBox = new VBox();
                            avatarBox.setAlignment(Pos.CENTER);
                            avatarBox.getChildren().add(avatarImage);
                            HBox hBoxAvatar = new HBox();
                            hBoxAvatar.getChildren().add(avatarBox);

                            VBox messageContentBox = (VBox) mainPane.lookup("#messageContentBox");
                            VBox vbox = addMessageBox(hBoxAvatar, messageContentBox, lastMessage, currentUser);
                            ScrollPane scrollPane = addMessagesScrollPane(mainPageController, vbox, initialConversation, page);
                            mainPane.setCenter(scrollPane);
                        });

                    });
                }
            }
        }
    }

    public VBox addMessageBox(HBox hBoxAvatar, VBox messageContentBox, Message message, User currentUser) {
        hBoxAvatar.setMaxHeight(50.);
        HBox hBoxMessage = new HBox();
        hBoxMessage.getStyleClass().add("boxMessage");
        Platform.runLater(() -> {
            VBox vBoxSender = new VBox();
            vBoxSender.setAlignment(Pos.CENTER_LEFT);
            HBox hBoxSender = new HBox();
            hBoxSender.getStyleClass().add("hBoxSenderMessage");

            Label labelSender = new Label();
            LocalDateTime date = message.getTime();

            if (StringUtils.equals(message.getSender().getEmailAddress(), currentUser.getEmailAddress())) {
                labelSender.setText("You");
                vBoxSender.getStyleClass().add("myMessageBox");
            } else {
                labelSender.setText(message.getSender().getDisplayName());
                vBoxSender.getStyleClass().add("senderMessageBox");
            }
            Label emailAddressSender = new Label();
            emailAddressSender.setText(message.getSender().getEmailAddress());
            emailAddressSender.getStyleClass().add("emailAddressMessage");
            emailAddressSender.setManaged(false);
            emailAddressSender.setVisible(false);
            labelSender.getStyleClass().add("labelSender");
            hBoxSender.getChildren().addAll(labelSender, emailAddressSender);

            Label space = new Label(" ");
            hBoxSender.getChildren().add(space);

            Label labelTime = new Label(DateHandler.transformDate(date));
            labelTime.getStyleClass().add("labelText");
            hBoxSender.getChildren().add(labelTime);

            vBoxSender.getChildren().add(hBoxSender);

            Text messageText = new Text(message.getContent());
            messageText.getStyleClass().add("conversationMessageText");
            messageText.setWrappingWidth(Math.max(200,
                    Math.min(messageText.getLayoutBounds().getWidth(), 600)));
            TextFlow messageTextFlow = new TextFlow();
            messageTextFlow.setMaxWidth(600);
            messageTextFlow.setMinWidth(Region.USE_PREF_SIZE);
            messageTextFlow.getChildren().add(messageText);
            messageTextFlow.setLineSpacing(2); //For better readability

            vBoxSender.getChildren().add(messageText);

            StackPane avatarStackPane = new StackPane();
            avatarStackPane.getStyleClass().add("stackPaneMessage");

            if (!StringUtils.equals(message.getSender().getEmailAddress(), currentUser.getEmailAddress())) {
                messageText.setFill(Color.WHITE);
                hBoxMessage.getChildren().addAll(avatarStackPane, vBoxSender);
            } else {
                hBoxMessage.getChildren().addAll(vBoxSender, avatarStackPane);
                hBoxMessage.setAlignment(Pos.BASELINE_RIGHT);
            }
            avatarStackPane.getChildren().add(hBoxAvatar);
            Region region = new Region();
            region.setMinHeight(10);
            messageContentBox.getChildren().addAll(region, hBoxMessage);
        });
        return messageContentBox;
    }
    
    public HBox addConversationTopBox(Set<User> participants, User currentUser) {
        HBox hBox = new HBox();
        hBox.getStyleClass().add("topConversationBox");
        hBox.setPrefHeight(40.);
        hBox.setMaxHeight(40.);
        hBox.setMinHeight(40.);
        hBox.setAlignment(Pos.CENTER);

        HBox targetUserInfo = new HBox();
        targetUserInfo.getStyleClass().add("topConversationLabels");

        int i = 0;
        for (User participant : participants) {
            if (!StringUtils.equals(currentUser.getEmailAddress(), participant.getEmailAddress())) {
                if (i == 0) {
                    try {
                        String correlationId = UUID.randomUUID().toString();
                        mainPageService.sendAvatarInfo(correlationId, participant);
                        ImageView avatarView = Listener.getServerReader().getAvatarByCorrelationId(correlationId);
                        avatarView.setFitHeight(40.);
                        avatarView.setFitWidth(40.);
                        targetUserInfo.getChildren().add(avatarView);
                    } catch (Exception e) {
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
        
        return hBox;
    }

    public ScrollPane addConversationMessagesScrollPane(MainPageController mainPageController, Conversation conversation, GridPane gridMainPane,
                                                        User currentUser, Page page, ScrollPane scrollPane) {
        VBox messageContentBox = new VBox();
        messageContentBox.setId("messageContentBox");
        messageContentBox.setPadding(new Insets(10, 10, 10, 10));
        Platform.runLater(() -> {
        for (Message message : conversation.getMessages()) {
            // Asynchronous avatar loading
            //Platform.runLater(() -> {
                try {
                    HBox hBoxAvatar = new HBox();
                    String correlationId = UUID.randomUUID().toString();
                    mainPageService.sendAvatarInfo(correlationId, message.getSender());
                    VBox avatarBox = new VBox();

                    ImageView imageView = Listener.getServerReader().getAvatarByCorrelationId(correlationId);
                    imageView.setFitWidth(40.);
                    imageView.setFitHeight(40.);
                    avatarBox.getChildren().add(imageView);
                    avatarBox.setAlignment(Pos.CENTER);
                    hBoxAvatar.getChildren().add(avatarBox);
                    addMessageBox(hBoxAvatar, messageContentBox, message, currentUser);
                    AnimationUtil.fadeInNode(messageContentBox);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            //});
        }});

        this.scrollPane = scrollPane;

        if (this.scrollPane == null) {
            this.scrollPane = addMessagesScrollPane(mainPageController, messageContentBox, conversation, page);
        }
        else {
            Platform.runLater(() -> {
                this.scrollPane.setContent(messageContentBox);
            });
        }

       return this.scrollPane;
    }
    
    public HBox addConversationSendMessageBox(MainPageController mainPageController, Conversation conversation,
                                              ConversationAction conversationAction) {
        HBox hBox1 = new HBox();
        hBox1.setId("sendBox");
        hBox1.setAlignment(Pos.CENTER);

        TextArea messageField = new TextArea();
        messageField.setWrapText(true);
        messageField.setId("sendMessage");
        messageField.getStyleClass().add("sendMessageField");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(40);
        imageView.setFitWidth(40);
        Image image = new Image(VoiceChatApplication.class.getResourceAsStream("images/send-button.png"));
        imageView.setId("sendButton");
        imageView.setImage(image);

        if (conversationAction == ConversationAction.CONTINUE) {
            mainPageController.sendMessageToExistingConversation(imageView, conversation);
        }

        Region spacer = new Region();
        spacer.setPrefWidth(10);
        hBox1.getChildren().addAll(messageField, spacer, imageView);
        
        return hBox1;
    }

    public void setMessagesComponents(MainPageController mainPageController, GridPane gridMainPane,
                                      VBox rightSearchPane, BorderPane mainPane, ServerResponse response,
                                      ConversationListComponent conversationListComponent,
                                      MessagesNotificationScheduler messagesNotificationScheduler) throws IOException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Conversation conversation = mapper.readValue(response.getBinaryPayload(), Conversation.class);
        Platform.runLater(() -> {
            User currentUser = UserSession.INSTANCE.getUser();
            
            // TOP (Conversation info box)
            mainPane.setTop(addConversationTopBox(conversation.getParticipants(), currentUser));

            // MIDDLE (messages)
            mainPane.setCenter(addConversationMessagesScrollPane(mainPageController, conversation, gridMainPane, currentUser, new Page(), this.scrollPane));

            // Bottom send box
            mainPane.setBottom(addConversationSendMessageBox(mainPageController, conversation, ConversationAction.CONTINUE));

            mainPane.setPadding(new Insets(0, 0, 10, 0));

            messagesNotificationScheduler.schedule(currentUser.getEmailAddress(), mainPageController, mainPane, mainPageController.getLeftPane(), this,
                   conversationListComponent, conversation, gridMainPane, onlineUsersScheduler);

            // RIGHT pane
            conversationMessageSearchComponent.setSearchMessageField(rightSearchPane, mainPageController, conversation);
        });
    }

    public ScrollPane addMessagesScrollPane(MainPageController mainPageController, VBox vBox, Conversation conversation,
                                            Page page) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setId("conversationScrollPane");
        scrollPane.setContent(vBox);
        scrollPane.fitToHeightProperty().set(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scrollBar");

        // Delay the scroll to bottom until after layout is complete
        Platform.runLater(() -> {
            // Ensure content height is valid
            if (vBox.getHeight() > 0) {
                scrollPane.setVvalue(1.0);
            } else {
                // In case height isn't set yet, add a listener
                vBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                    scrollPane.setVvalue(1.0);
                });
            }
            // Calculate the target vvalue based on current content height
            Platform.runLater(() -> {
                VBox messageBox = (VBox) scrollPane.lookup("#messageContentBox");
                if (messageBox != null) {
                    double contentHeight = messageBox.getHeight();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();
                    double targetVvalue = (contentHeight - viewportHeight) > 0 ? scrollPane.getVvalue() : 1.0;

                    // For example, scroll to bottom:
                    targetVvalue = 1.0;

                    // Animate to the target position
                    AnimationUtil.smoothVScrollTo(scrollPane, targetVvalue);

                    // Then, load more messages after the animation completes
                    // Optionally, you can set a delay or listen for animation completion
                    // But for simplicity, just call loadMoreMessages after a small delay
                    Timeline delay = new Timeline(new KeyFrame(Duration.millis(300), e -> {
                        loadMoreMessages(mainPageController, scrollPane, conversation);
                    }));
                    delay.play();
                }
            });
        });

        return scrollPane;
    }

    public void loadMoreMessages(MainPageController mainPageController, ScrollPane scrollPane, Conversation conversation) {
        PauseTransition pause = new PauseTransition(Duration.millis(200));
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            pause.stop();
            pause.setOnFinished(ev -> {

                double oldContentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                double oldViewportHeight = scrollPane.getViewportBounds().getHeight();
                double pixelPosition = scrollPane.getVvalue() * (oldContentHeight - oldViewportHeight);

                var box = (VBox) scrollPane.lookup("#messageContentBox");
                double oldHeight = box.getHeight();
                // Save current pixel position of the viewport

                int offset = page.getOffset();
                int limit = page.getLimit();

                if (scrollPane.getVvalue() >= 1.0) {
                    if (event.getDeltaY() < 0) {
                        if (offset >= 0) {
                            // Scrolling down at bottom
                            if (limit >= 60) {
                                if (offset >= 0) {
                                    limit = 20;
                                    --offset;
                                    page.setLimit(limit);
                                    page.setOffset(offset);
                                    page.setScrollDirection(ScrollDirection.BOTTOM);
                                    mainPageController.scrollConversationMessages(conversation, page);
                                }
                            } else {
                                limit += 20;
                                page.setLimit(limit);
                                page.setOffset(offset);
                                page.setScrollDirection(ScrollDirection.BOTTOM);
                                mainPageController.loadMoreConversationMessages(conversation, page, scrollPane, () -> {
                                    listenAndSetVvalue(scrollPane);
                                });
                            }
                        }
                    }
                } else if (scrollPane.getVvalue() <= 0.0) {
                    if (event.getDeltaY() > 0) {
                        // Scrolling up at top
                        if (limit >= 60) {
                            limit = 20;
                            ++offset;
                            page.setLimit(limit);
                            page.setOffset(offset);
                            page.setScrollDirection(ScrollDirection.TOP);
                            mainPageController.scrollConversationMessages(conversation, page);
                        } else {
                            limit += 20;
                            page.setLimit(limit);
                            page.setOffset(offset);
                            page.setScrollDirection(ScrollDirection.TOP);
                            mainPageController.loadMoreConversationMessages(conversation, page, scrollPane, () -> {
                                listenAndSetVvalue(scrollPane);
                            });
                        }
                    }
                }
            });
            pause.playFromStart();
        });

    }

    public void listenAndSetVvalue(ScrollPane scrollPane) {
        scrollPane.contentProperty().addListener((obs, oldContent, newContent) -> {
            if (newContent != null) {
                // Wait for layout pass to complete
                newContent.boundsInParentProperty().addListener((observable, oldBounds, newBounds) -> {
                    if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                        Node content = scrollPane.getContent();
                        if (content != null) {
                            var messageBox = (VBox) scrollPane.lookup("#messageContentBox");
                            vvalue = (messageBox.getHeight() - scrollPane.getViewportBounds().getHeight())/messageBox.getHeight();
                        }
                        if (scrollPane.getVvalue() <= 0.0 && vvalue > 0 && vvalue < 1) {
                            Platform.runLater(() -> {
                                smoothScrollTo(scrollPane, vvalue);
                            });
                        }
                        else if (scrollPane.getVvalue() >= 1. && vvalue > 0 && vvalue < 1) {
                            Platform.runLater(() -> {
                                smoothScrollTo(scrollPane, vvalue);
                            });
                        }
                    }
                });
            }
        });
    }

    public void smoothScrollTo(ScrollPane scrollPane, double targetVvalue) {
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(scrollPane.vvalueProperty(), targetVvalue);
        KeyFrame kf = new KeyFrame(Duration.millis(300), kv); // 300ms for smoothness
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    public void newConversationComponents(User targetUser, MainPageController parentController,
                                          GridPane gridPane, Pane searchPane) {
        Platform.runLater(() -> {
            BorderPane mainPane = (BorderPane) gridPane.lookup("#mainPane");
            searchPane.getChildren().clear();
            User currentUser = UserSession.INSTANCE.getUser();

            // TOP (Conversation info box)
            mainPane.setTop(addConversationTopBox(Set.of(currentUser, targetUser), currentUser));

            // MIDDLE (messages)
            mainPane.setCenter(addConversationMessagesScrollPane(parentController, new Conversation(), gridPane, currentUser, new Page(),
                    this.scrollPane));

            // Bottom send box
            mainPane.setBottom(addConversationSendMessageBox(parentController, null, ConversationAction.START));

            parentController.sendMessage();
        });
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

}
