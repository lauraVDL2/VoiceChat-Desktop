package com.voicechat.client.mainpage.controller;

import com.voicechat.client.VoiceChatApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import java.io.IOException;

public class MainPageController {

    @FXML
    private VBox leftPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private GridPane gridMainPane;

    private HeaderController headerController;

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
