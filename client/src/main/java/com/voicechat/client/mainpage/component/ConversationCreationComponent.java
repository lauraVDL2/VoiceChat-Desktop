package com.voicechat.client.mainpage.component;

import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.controller.MainPageController;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ConversationCreationComponent {

    private final ConversationComponent conversationComponent = new ConversationComponent();

    private final Set<String> selectedEmails = new HashSet<>();

    public void createConversationBoxes(MainPageController mainPageController,
                                        ImageView plusImage, BorderPane mainPane) {
        plusImage.setOnMouseClicked((mouseEvent -> {
            mainPane.getChildren().clear();
            VBox mainBox = new VBox();
            mainBox.setAlignment(Pos.TOP_CENTER);
            addTopBox(mainBox, mainPageController);
            mainPane.setCenter(mainBox);
            mainPane.setBottom(conversationComponent.addConversationSendMessageBox(mainPageController, null,
                    ConversationAction.START));
            ImageView imageView = (ImageView) mainPane.lookup("#sendButton");
            TextArea textField = (TextArea) mainPane.lookup("#sendMessage");
            imageView.setOnMouseClicked((mouseEvent2 -> {
                User currentUser = UserSession.INSTANCE.getUser();
                if (currentUser != null) {
                    Message message = new Message();
                    message.setContent(textField.getText());
                    message.setSender(currentUser);
                    message.setTime(LocalDateTime.now());
                    List<ReadStatus> readStatuses = new ArrayList<>();
                    List<User> users = new ArrayList<>();
                    for (String email : selectedEmails) {
                        User user = new User();
                        user.setEmailAddress(email);
                        users.add(user);
                        ReadStatus readStatus = new ReadStatus(false, message, user);
                        readStatuses.add(readStatus);
                    }
                    users.add(currentUser);
                    ReadStatus readStatus = new ReadStatus(true, message, currentUser);
                    readStatuses.add(readStatus);
                    message.setReadStatuses(readStatuses);
                    mainPageController.getOrCreateConversationIfExists(new HashSet<>(users), message);
                }
            }));
        }));
    }

    public void addTopBox(VBox mainBox, MainPageController mainPageController) {
        HBox topBox = new HBox();

        TextField searchField = new TextField();
        searchField.getStyleClass().add("searchUserField");
        topBox.getChildren().add(searchField);
        mainBox.getChildren().add(topBox);

        searchField.setOnKeyPressed(keyEvent -> {
            mainPageController.searchUsers(topBox, mainBox, searchField);
        });
    }

    public void setSearchUsersContent(MainPageController mainPageController, VBox mainBox, HBox topBox, List<User> users) {
        Platform.runLater(() -> {
            mainBox.getChildren().removeIf(node -> node instanceof VBox);
            VBox vBox = new VBox();
            for (User user : users) {
                if (selectedEmails.contains(user.getEmailAddress()) || StringUtils.equals(user.getEmailAddress(),
                        UserSession.INSTANCE.getUser().getEmailAddress())) {
                    continue;
                }
                HBox hBox = new HBox();
                hBox.getStyleClass().add("searchLabel");
                Label displayLabel = new Label();
                displayLabel.setText(user.getDisplayName());
                displayLabel.setLineSpacing(3);
                Label emailAddress = new Label();
                emailAddress.setText("(" + user.getEmailAddress() + ")");
                emailAddress.setLineSpacing(3);
                hBox.setUserData(user.getEmailAddress());
                hBox.toFront();
                hBox.getChildren().add(displayLabel);
                hBox.getChildren().add(emailAddress);
                vBox.toFront();
                vBox.getChildren().add(hBox);
            }
            mainBox.getChildren().add(vBox);
            for (var node : vBox.getChildren()) {
                node.setOnMouseClicked((mouseEvent -> {
                    selectUser(mainBox, topBox, mainPageController, (String) node.getUserData());
                }));
            }
        });
    }

    public void selectUser(VBox mainBox, HBox topBox, MainPageController mainPageController, String emailAddress) {
        mainBox.getChildren().removeIf(node -> node instanceof VBox);
        topBox.getChildren().clear();
        Label emailAddressLabel = new Label(emailAddress);
        topBox.getChildren().add(emailAddressLabel);

        selectedEmails.add(emailAddress);

        addTopBox(mainBox, mainPageController);
    }

}
