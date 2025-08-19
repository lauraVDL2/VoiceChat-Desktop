package com.voicechat.client.mainpage;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

public class MainPageController {

    @FXML
    private VBox leftPane;

    @FXML
    private SplitPane splitPane;

    @FXML
    public void initialize() {

    }

    public void initializeSplitPane() {
        Platform.runLater(() -> {
            double totalWidth = splitPane.getWidth(); // or scene width
            double dividerPos = 350 / totalWidth;
            splitPane.setDividerPositions(dividerPos);
            leftPane.setMinWidth(200);
        });
    }
}
