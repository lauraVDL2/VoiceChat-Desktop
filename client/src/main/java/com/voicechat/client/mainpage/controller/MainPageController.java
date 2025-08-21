package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPageController {

    @FXML
    private VBox leftPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private TextField searchField;
    @FXML
    private Pane searchPane;
    @FXML
    private HBox topPane;
    @FXML
    private ImageView searchIcon;
    @FXML
    private GridPane gridMainPane;
    @FXML
    private StackPane searchStackPane;
    @FXML
    private ImageView myAvatar;

    private final MainPageService mainPageService = new MainPageService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        initializePane();
        searchUsers();
        gridPaneFocus();
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

            // Left Pane
            double totalWidth = splitPane.getWidth(); // or scene width
            double dividerPos = 350 / totalWidth;
            splitPane.setDividerPositions(dividerPos);
            leftPane.setMinWidth(200);
            topPane.toFront();
        });
    }

    public void initializeAvatar() throws IOException {
        DataInputStream dataInputStream = Listener.getDataInputStream();
        int size = dataInputStream.readInt();
        if (size > 0) {
            byte[] imageBytes = new byte[size];
            dataInputStream.readFully(imageBytes); // read the image bytes

            // Convert to Image
            Image image = new Image(new ByteArrayInputStream(imageBytes));

            myAvatar.setImage(image);
        }
    }

    public void gridPaneFocus() {
        var children = gridMainPane.getChildren();
        for (var child : children) {
            child.setOnMouseClicked(mouseEvent -> {
                child.requestFocus();
                searchPane.getChildren().clear();
            });
        }
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
                    return mainPageService.searchUser(searchDisplayName);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }, executor).thenAcceptAsync((serverResponse) -> {
                if (serverResponse != null) {
                    if (serverResponse.getServerResponseStatus() == ServerResponseStatus.SUCCESS) {
                        if (serverResponse.getServerResponseMessage() == ServerResponseMessage.USER_SEARCHED) {
                            try {
                                ObjectMapper objectMapper = new ObjectMapper();
                                List<User> users = objectMapper.readValue(serverResponse.getPayload(),
                                        new TypeReference<List<User>>() {
                                        });
                                // Update UI on JavaFX thread
                                Platform.runLater(() -> {
                                    VBox vBox = new VBox();
                                    for (User user : users) {
                                        Label label = new Label();
                                        label.getStyleClass().add("searchLabel");
                                        label.setText(user.getDisplayName());
                                        label.setLineSpacing(3);
                                        vBox.toFront();
                                        vBox.getChildren().add(label);
                                    }
                                    searchPane.getChildren().add(vBox);
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, executor);
        });
    }
}
