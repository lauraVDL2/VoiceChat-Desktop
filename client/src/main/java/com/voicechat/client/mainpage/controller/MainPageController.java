package com.voicechat.client.mainpage.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.login.UserSession;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import org.apache.commons.collections4.CollectionUtils;
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

public class MainPageController {

    @FXML
    private VBox leftPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private GridPane gridMainPane;
    @FXML
    private BorderPane mainPane;

    private HeaderController headerController;

    private final MainPageService mainPageService = new MainPageService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(VoiceChatApplication.class.getResource("mainpage/header.fxml"));
                Parent childNode = loader.load();
                headerController = loader.getController();

                gridMainPane.getChildren().add(childNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
            double totalWidth = splitPane.getWidth();
            if (totalWidth > 0) {
                double dividerPos = 350 / totalWidth;
                splitPane.setDividerPositions(dividerPos);
            }
            leftPane.setMinWidth(200);
            gridPaneFocus();
        });
    }

    public void gridPaneFocus() {
        var children = gridMainPane.getChildren();
        for (var child : children) {
            child.setOnMouseClicked(mouseEvent -> {
                child.requestFocus();
                headerController.getSearchField().clear();
            });
        }
    }
}
