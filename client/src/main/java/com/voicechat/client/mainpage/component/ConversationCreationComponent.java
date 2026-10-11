package com.voicechat.client.mainpage.component;

import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.controller.MainPageController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
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

    private FlowPane recipientChips;
    private TextField searchField;
    private VBox resultsBox;

    public void createConversationBoxes(MainPageController controller,
                                        ImageView plusImage,
                                        BorderPane mainPane) {
        plusImage.setOnMouseClicked(event -> {
            selectedEmails.clear();

            VBox composer = buildComposer(controller);
            mainPane.setCenter(composer);
            mainPane.setBottom(conversationComponent.addConversationSendMessageBox(
                    controller, null, ConversationAction.START));

            ImageView sendButton = (ImageView) mainPane.lookup("#sendButton");
            TextArea messageArea = (TextArea) mainPane.lookup("#sendMessage");

            if (sendButton != null && messageArea != null) {
                sendButton.setOnMouseClicked(sendEvent ->
                        sendMessage(controller, messageArea));
            }
        });
    }

    private VBox buildComposer(MainPageController controller) {
        VBox composer = new VBox(18);
        composer.getStyleClass().add("group-composer");
        composer.setPadding(new Insets(24));
        composer.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("New message");
        title.getStyleClass().add("group-composer-title");

        Label subtitle = new Label("Add people to start a group conversation");
        subtitle.getStyleClass().add("group-composer-subtitle");

        HBox recipientRow = new HBox(10);
        recipientRow.setAlignment(Pos.CENTER_LEFT);
        recipientRow.getStyleClass().add("recipient-row");

        Label toLabel = new Label("To");
        toLabel.getStyleClass().add("recipient-to-label");

        recipientChips = new FlowPane(8, 8);
        recipientChips.setAlignment(Pos.CENTER_LEFT);
        recipientChips.getStyleClass().add("recipient-chips");

        searchField = new TextField();
        searchField.setPromptText("Search for people...");
        searchField.getStyleClass().add("recipient-search");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        recipientRow.getChildren().addAll(toLabel, recipientChips, searchField);

        resultsBox = new VBox(4);
        resultsBox.getStyleClass().add("user-search-results");

        ScrollPane resultsScroll = new ScrollPane(resultsBox);
        resultsScroll.setFitToWidth(true);
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScroll.setMaxHeight(320);
        resultsScroll.getStyleClass().add("user-search-scroll");

        searchField.setOnKeyReleased(event -> {
            if (!searchField.getText().isBlank()) {
                HBox searchContext = new HBox();
                controller.searchUsers(searchContext, composer, searchField);
            } else {
                resultsBox.getChildren().clear();
            }
        });

        composer.getChildren().addAll(title, subtitle, recipientRow, resultsScroll);
        return composer;
    }

    public void setSearchUsersContent(MainPageController controller,
                                      VBox mainBox,
                                      HBox topBox,
                                      List<User> users) {
        Platform.runLater(() -> {
            if (resultsBox == null) {
                return;
            }

            resultsBox.getChildren().clear();

            User currentUser = UserSession.INSTANCE.getUser();
            for (User user : users) {
                if (user == null
                        || StringUtils.equals(user.getEmailAddress(),
                        currentUser == null ? null : currentUser.getEmailAddress())
                        || selectedEmails.contains(user.getEmailAddress())) {
                    continue;
                }

                HBox row = createUserRow(user);
                row.setOnMouseClicked(event -> selectUser(user));
                resultsBox.getChildren().add(row);
            }

            if (resultsBox.getChildren().isEmpty()) {
                Label empty = new Label("No people found");
                empty.getStyleClass().add("search-empty-label");
                resultsBox.getChildren().add(empty);
            }
        });
    }

    private HBox createUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("user-search-row");

        Label avatar = new Label(initials(user.getDisplayName()));
        avatar.getStyleClass().add("user-avatar");

        VBox details = new VBox(3);
        Label name = new Label(user.getDisplayName());
        name.getStyleClass().add("user-name");

        Label email = new Label(user.getEmailAddress());
        email.getStyleClass().add("user-email");

        details.getChildren().addAll(name, email);
        row.getChildren().addAll(avatar, details);
        return row;
    }

    private void selectUser(User user) {
        String email = user.getEmailAddress();
        if (email == null || !selectedEmails.add(email)) {
            return;
        }

        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER);
        chip.getStyleClass().add("recipient-chip");

        Label name = new Label(user.getDisplayName());
        Label remove = new Label("×");
        remove.getStyleClass().add("recipient-chip-remove");
        remove.setOnMouseClicked(event -> {
            selectedEmails.remove(email);
            recipientChips.getChildren().remove(chip);
            searchField.requestFocus();
        });

        chip.getChildren().addAll(name, remove);
        recipientChips.getChildren().add(chip);

        searchField.clear();
        resultsBox.getChildren().clear();
        searchField.requestFocus();
    }

    private void sendMessage(MainPageController controller, TextArea messageArea) {
        User currentUser = UserSession.INSTANCE.getUser();
        String content = messageArea.getText();

        if (currentUser == null || selectedEmails.isEmpty()
                || content == null || content.isBlank()) {
            return;
        }

        Message message = new Message();
        message.setContent(content.trim());
        message.setSender(currentUser);
        message.setTime(LocalDateTime.now());

        List<User> users = new ArrayList<>();
        List<ReadStatus> readStatuses = new ArrayList<>();

        for (String email : selectedEmails) {
            User user = new User();
            user.setEmailAddress(email);
            users.add(user);
            readStatuses.add(new ReadStatus(false, message, user));
        }

        users.add(currentUser);
        readStatuses.add(new ReadStatus(true, message, currentUser));
        message.setReadStatuses(readStatuses);

        controller.getOrCreateConversationIfExists(new HashSet<>(users), message);
    }

    private String initials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }

        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }
}