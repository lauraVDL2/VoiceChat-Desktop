package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.service.HeaderService;
import com.voicechat.client.utils.JsonMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.apache.commons.lang3.StringUtils;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            topPane.toFront();
        });
    }

    public void initializeAvatar() throws IOException {
        CompletableFuture.runAsync(() -> {
            try {
                DataInputStream dataInputStream = Listener.getDataInputStream();
                int size = dataInputStream.readInt();
                if (size > 0) {
                    byte[] imageBytes = new byte[size];
                    dataInputStream.readFully(imageBytes);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    Platform.runLater(() -> myAvatar.setImage(image));
                }
            } catch (IOException e) {
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
                } catch (IOException e) {
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

    public void searchUserComponents(ServerResponse serverResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        List<User> users = objectMapper.readValue(serverResponse.getPayload(),
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
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executor).thenAcceptAsync((serverResponse) -> {
                    if (serverResponse != null) {
                        if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                            if (serverResponse.getServerResponseMessage() == ServerResponseMessage.CONVERSATION_SEARCHED) {
                                System.out.println("conversation exists !");
                            }
                        }
                        else if (serverResponse.getServerResponseStatus() == ServerResponseStatus.INFO) {
                            newConversationComponents(targetUser);
                        }
                        else {
                            System.out.println("ERROR");
                        }
                    }
                }, executor);
            }
        }));
    }

    public void newConversationComponents(User targetUser) {
        Platform.runLater(() -> {
            GridPane gridPane = (GridPane) topPane.getParent();
            SplitPane splitPane = (SplitPane) gridPane.getChildren().stream()
                    .filter(child -> child instanceof SplitPane && StringUtils.equals(child.getId(), "splitPane"))
                    .findFirst().orElse(null);
            BorderPane borderPane = (BorderPane) splitPane.getItems().stream()
                    .filter(child -> child instanceof BorderPane && StringUtils.equals(child.getId(), "mainPane"))
                    .findFirst().orElse(null);
            searchPane.getChildren().clear();

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
            Label displayNameLabel = new Label();
            displayNameLabel.setId("displayNameLabelConv");
            targetUserInfo.getChildren().add(introLabel);
            displayNameLabel.setText(targetUser.getDisplayName());
            targetUserInfo.getChildren().add(displayNameLabel);
            Label emailAddressLabel = new Label();
            emailAddressLabel.setText("(" + targetUser.getEmailAddress() + ")");
            emailAddressLabel.setId("emailAddressLabelConv");
            targetUserInfo.setAlignment(Pos.CENTER);
            targetUserInfo.getChildren().add(emailAddressLabel);
            HBox.setMargin(targetUserInfo, new Insets(0, 0, 0, 30));
            hBox.getChildren().add(targetUserInfo);

            HBox optionsBox = new HBox();
            HBox.setHgrow(optionsBox, Priority.ALWAYS);
            optionsBox.setAlignment(Pos.CENTER_RIGHT);
            TextField searchMessage = new TextField();
            optionsBox.getChildren().add(searchMessage);
            hBox.getChildren().add(optionsBox);
            borderPane.setTop(hBox);

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
            borderPane.setBottom(hBox1);

            this.parentController.sendMessage();
        });
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
