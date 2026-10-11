package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.component.DotsMenuComponent;
import com.voicechat.client.mainpage.component.UserSearchComponent;
import com.voicechat.client.mainpage.service.HeaderService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeaderController {
    @FXML
    private TextField searchField;
    @FXML
    private Pane searchPane;
    @FXML
    private Pane dotsMenuPane;
    @FXML
    private HBox topPane;
    @FXML
    private ImageView searchIcon;
    @FXML
    private StackPane searchStackPane;
    @FXML
    private ImageView myAvatar;
    @FXML
    private ImageView threeDots;

    private final ServerReader serverReader = Listener.getServerReader();
    private final HeaderService headerService = new HeaderService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MainPageController parentController;

    private final ConversationComponent conversationComponent =
            new ConversationComponent();
    private final ConversationListComponent conversationListComponent =
            new ConversationListComponent();
    private final DotsMenuComponent dotsMenuComponent =
            new DotsMenuComponent();
    private final UserSearchComponent searchUserComponent =
            new UserSearchComponent();

    @FXML
    public void initialize() {
        initializePane();
        searchUsers();

        topPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                dotsMenuComponent.setDotsMenu(
                        threeDots, dotsMenuPane, newScene, this
                );
            }
        });
    }

    public void initializePane() {
        Platform.runLater(() -> {
            var searchResource =
                    VoiceChatApplication.class.getResourceAsStream("images/search.png");

            if (searchResource != null) {
                searchIcon.setImage(new Image(searchResource));
            }

            searchStackPane.setMargin(searchIcon, new Insets(0, 0, 0, 5));

            initializeAvatar();
            topPane.toFront();
        });
    }

    public void initializeAvatar() {
        CompletableFuture.runAsync(() -> {
            try {
                String correlationId = UUID.randomUUID().toString();
                headerService.sendAvatarInfo(
                        correlationId, UserSession.INSTANCE.getUser()
                );

                ImageView avatar =
                        serverReader.getAvatarByCorrelationId(correlationId);

                if (avatar == null
                        || avatar.getImage() == null
                        || avatar.getImage().isError()) {
                    System.err.println(
                            "Avatar response timed out or contained no valid image"
                    );
                    return;
                }

                Platform.runLater(() -> {
                    myAvatar.setImage(avatar.getImage());
                    myAvatar.setFitWidth(40);
                    myAvatar.setFitHeight(40);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public void searchUsers() {
        searchField.setOnKeyReleased(event -> {
            String query = searchField.getText().trim();

            if (query.isEmpty()) {
                clearSearchField();
                return;
            }

            CompletableFuture.supplyAsync(() -> {
                try {
                    return headerService.searchUser(query);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAccept(response -> {
                if (response == null) {
                    return;
                }

                if (response.getServerResponseStatus()
                        == ServerResponseStatus.SUCCESS
                        && response.getServerResponseMessage()
                        == ServerResponseMessage.USER_SEARCHED) {
                    try {
                        searchUserComponents(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    public List<User> deserializeUsers(ServerResponse response)
            throws IOException {
        ObjectMapper mapper = JsonMapper.getJsonMapper();

        return mapper.readValue(
                response.getBinaryPayload(),
                new TypeReference<List<User>>() {}
        );
    }

    private void searchUserComponents(ServerResponse response)
            throws IOException {
        List<User> users = deserializeUsers(response);
        searchUserComponent.searchUserComponents(this, searchPane, users);
    }

    public void startConversation(User targetUser) {
        User currentUser = UserSession.INSTANCE.getUser();
        List<User> users = List.of(currentUser, targetUser);

        CompletableFuture.supplyAsync(() -> {
            try {
                return headerService.searchConversationIfExists(users);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, executor).thenAccept(response -> {
            if (response == null) {
                return;
            }

            if (response.getServerResponseStatus()
                    == ServerResponseStatus.SUCCESS
                    && response.getServerResponseMessage()
                    == ServerResponseMessage.CONVERSATION_SEARCHED) {
                Platform.runLater(() -> {
                    try {
                        conversationListComponent.setConversationClicked(
                                parentController.getLeftPane(), response
                        );
                        conversationComponent.setMessagesComponents(
                                parentController,
                                parentController.getGridMainPane(),
                                parentController.getRightSearchPane(),
                                parentController.getMainPane(),
                                response,
                                conversationListComponent,
                                parentController.getMessagesNotificationScheduler()
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else if (response.getServerResponseStatus()
                    == ServerResponseStatus.INFO) {
                Platform.runLater(() ->
                        conversationComponent.newConversationComponents(
                                targetUser,
                                parentController,
                                parentController.getGridMainPane(),
                                searchPane
                        )
                );
            }
        });
    }

    public void clearSearchField() {
        searchPane.getChildren().clear();
    }

    public TextField getSearchField() {
        return searchField;
    }

    public void setParentController(MainPageController mainPageController) {
        this.parentController = mainPageController;
    }

    public MainPageController getParentController() {
        return parentController;
    }
}