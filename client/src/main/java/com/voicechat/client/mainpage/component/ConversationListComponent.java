package com.voicechat.client.mainpage.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.common.component.MarginComponent;
import com.voicechat.client.login.component.AuthenticatePopupComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import com.voicechat.client.common.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.entity.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationListComponent {

    private final MainPageService mainPageService = new MainPageService();

    private final AvatarComponent avatarComponent = new AvatarComponent();

    private final MarginComponent marginComponent = new MarginComponent();

    //private final ConversationCreationComponent conversationCreationComponent = new ConversationCreationComponent();

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

    public void setConversationList(MainPageController mainPageController, BorderPane mainPane,
                                    VBox leftPane, ServerResponse serverResponse) {
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

            TitledPane accordion = new TitledPane();
            User user = UserSession.INSTANCE.getUser();
            Image accordionImage = new Image(VoiceChatApplication.class.
                    getResourceAsStream("/com/voicechat/client/images/person.png"));
            Image plusImage = new Image(VoiceChatApplication.class.
                    getResourceAsStream("/com/voicechat/client/images/plus_black.png"));
            if (user.getSettings() != null) {
                if (user.getSettings().getThemeMode() == ThemeMode.DARK) {
                    accordionImage = new Image(VoiceChatApplication.class.
                            getResourceAsStream("/com/voicechat/client/images/person_white_icon.png"));
                    plusImage = new Image(VoiceChatApplication.class.
                            getResourceAsStream("/com/voicechat/client/images/plus_white.png"));
                }
            }

            HBox imageBox = new HBox();
            imageBox.setAlignment(Pos.CENTER_RIGHT);
            ImageView plusImageView = new ImageView(plusImage);
            plusImageView.setFitHeight(25.);
            plusImageView.setFitWidth(25.);
            plusImageView.setMouseTransparent(false);
            plusImageView.setPickOnBounds(true); // Click anywhere within its bounds
            imageBox.getChildren().addAll(marginComponent.initHorizontalMargin(8), plusImageView,
                    marginComponent.initHorizontalMargin(8));

            ImageView accordionImageView = new ImageView(accordionImage);
            accordionImageView.setFitWidth(14.);
            accordionImageView.setFitHeight(14.);
            accordion.setGraphic(accordionImageView);
            accordion.getStyleClass().add("accordionStyle");
            List<StackPane> stackPanes = new ArrayList<>();

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
                                imageView.setFitWidth(35.);
                                imageView.setFitHeight(35.);
                                imageView.setPreserveRatio(true);
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

                //vBox.getChildren().add(addLastMessageLabel(lastMessage));

                vbox2.setAlignment(Pos.CENTER);
                vBox.setAlignment(Pos.CENTER);

                //mainVbox.getStyleClass().add("discussionBox");

                hBox.getChildren().addAll(vbox2, vBox);

//                HBox hBoxTime = new HBox();
//                VBox vboxTime = new VBox();
//                Label timeLabel = new Label();
//                timeLabel.setText(DateHandler.transformDate(lastMessage.getTime()));
//                timeLabel.getStyleClass().add("dateDiscussionLabel");
//                hBoxTime.setAlignment(Pos.CENTER);
//                hBoxTime.getStyleClass().add("dateDiscussion");

                //vboxTime.getChildren().add(timeLabel);
                //hBoxTime.getChildren().add(vboxTime);
                mainVbox.getChildren().add(hBox);
                //mainVbox.getChildren().addAll(hBoxTime, hBox);

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
                stackPanes.add(contentStackPane);

                circleUnread(unreadMessages, contentStackPane);

                mainPageController.createConversationBoxes(plusImageView);
                //conversationCreationComponent.createConversationBoxes(mainPageController, plusImageView, mainPane);
                mainPageController.goToConversation(contentStackPane);
            }
            VBox vBox = new VBox();
            for (var node : stackPanes) {
                vBox.getChildren().add(node);
            }
            accordion.setText("Conversations");
            accordion.setContent(vBox);
            leftPane.getChildren().addAll(marginComponent.initVerticalMargin(8), imageBox,
                    marginComponent.initVerticalMargin(8), accordion);
        });
    }

    public void circleUnread(Integer size, StackPane stackPane) {
        StackPane stackPaneCircle = new StackPane();
        Circle circle = new Circle(10);
        circle.getStyleClass().add("notificationCircle");
        //circle.setFill(Color.STEELBLUE);
        HBox hBox = new HBox(0);
        hBox.setPadding(new Insets(0, 10, 0, 10));
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
