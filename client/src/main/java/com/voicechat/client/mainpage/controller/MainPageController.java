package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;

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

    private final MainPageService mainPageService = new MainPageService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        initializeSplitPane();
        searchUsers();
    }

    public void initializeSplitPane() {
        Platform.runLater(() -> {
            double totalWidth = splitPane.getWidth(); // or scene width
            double dividerPos = 350 / totalWidth;
            splitPane.setDividerPositions(dividerPos);
            leftPane.setMinWidth(200);
            topPane.toFront();
        });
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
