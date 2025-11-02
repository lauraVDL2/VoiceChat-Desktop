package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.service.HeaderService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeaderController {
    @FXML
    private TextField searchField;
    @FXML
    private Pane searchPane;
    @FXML
    private HBox topPane;
    @FXML
    private ImageView searchIcon;
    @FXML
    private StackPane searchStackPane;
    @FXML
    private ImageView myAvatar;

    private final HeaderService headerService = new HeaderService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MainPageController parentController;

    private final ConversationComponent conversationComponent = new ConversationComponent();

    private final ConversationListComponent conversationListComponent = new ConversationListComponent();

    @FXML
    public void initialize() {
        initializePane();
        searchUsers();
    }

    public void initializePane() {
        Platform.runLater(() -> {
            // Top Pane
            Image searchImage = new Image(VoiceChatApplication.class.getResourceAsStream("images/search.png"));
            searchIcon.setImage(searchImage);
            searchStackPane.setMargin(searchIcon, new Insets(0, 0, 0, 5));

            try {
                initializeAvatar();
            } catch (Exception e) {
                e.printStackTrace();
            }
            topPane.toFront();
        });
    }

    public void initializeAvatar() {
        CompletableFuture.runAsync(() -> {
            try {
                myAvatar.setImage(Listener.getServerReader().getAvatar().getImage());
                myAvatar.setFitWidth(40.);
                myAvatar.setFitHeight(40.);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public void searchUsers() {
        searchField.setOnKeyPressed((event) -> {
            String searchDisplayName = searchField.getText().trim();

            searchPane.getChildren().clear();

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
                                searchUserComponents(serverResponse);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, executor);
        });
    }

    public void searchUserComponents(ServerResponse serverResponse) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        List<User> users = objectMapper.readValue(serverResponse.getBinaryPayload(),
                new TypeReference<List<User>>() {
                });
        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            VBox vBox = new VBox();
            for (User user : users) {
                HBox hBox = new HBox();
                hBox.getStyleClass().add("searchLabel");
                Label displayLabel = new Label();
                displayLabel.setText(user.getDisplayName());
                displayLabel.setLineSpacing(3);
                Label emailAddress = new Label();
                emailAddress.setText("(" + user.getEmailAddress() + ")");
                emailAddress.setLineSpacing(3);
                hBox.toFront();
                hBox.getChildren().add(displayLabel);
                hBox.getChildren().add(emailAddress);
                vBox.toFront();
                vBox.getChildren().add(hBox);
            }
            searchPane.getChildren().add(vBox);
            for (var hBox : vBox.getChildren()) {
                startConversation((HBox) hBox);
            }
        });
    }

    public void startConversation(HBox hBox) {
        hBox.setOnMouseClicked((mouseEvent -> {
            Node node = hBox.getChildren().get(1);
            Node displayNameNode = hBox.getChildren().get(0);
            if (node instanceof Label && displayNameNode instanceof Label) {
                Label label = (Label) node;
                Label displayNameLabel = (Label) displayNameNode;
                String targetEmailAddress = label.getText()
                        .replace("(", "").replace(")", "");
                String targetDisplayName = displayNameLabel.getText();
                org.shared.entity.User user = UserSession.INSTANCE.getUser();
                User targetUser = new User();
                targetUser.setEmailAddress(targetEmailAddress);
                targetUser.setDisplayName(targetDisplayName);
                List<User> users = new ArrayList<>();
                users.add(user);
                users.add(targetUser);
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return headerService.searchConversationIfExists(users);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync((serverResponse) -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_SEARCHED) {
                                System.out.println("conversation exists !");
                                try {
                                    conversationListComponent.setConversationClicked(parentController.getLeftPane(), serverResponse);
                                    conversationComponent.setMessagesComponents(parentController, parentController.getGridMainPane(),
                                            parentController.getRightSearchPane(), parentController.getMainPane(), serverResponse, conversationListComponent,
                                            parentController.getMessagesNotificationScheduler());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else if (serverResponse.getServerResponseStatus() == ServerResponseStatus.INFO) {
                            conversationComponent.newConversationComponents(targetUser, parentController, parentController.getGridMainPane(), searchPane);
                        }
                        else {
                            System.out.println("ERROR");
                        }
                    }
                }, executor);
            }
        }));
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
        return this.parentController;
    }

}
