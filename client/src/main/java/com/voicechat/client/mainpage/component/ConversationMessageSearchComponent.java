package com.voicechat.client.mainpage.component;

import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.common.utils.DateHandler;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.shared.entity.Conversation;
import org.shared.entity.Message;

import java.util.List;

public class ConversationMessageSearchComponent {

    public void setMessagesSearched(VBox rightSearchPane, List<Message> messages, Conversation conversation, MainPageController mainPageController) {
        Platform.runLater(() -> {

            setSearchMessageField(rightSearchPane, mainPageController, conversation);

            VBox messageList = new VBox();
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.fitToHeightProperty().set(true);
            scrollPane.setFitToWidth(true);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            messageList.getStyleClass().add("messageConversationSearchedList");

            for (Message message : messages) {
                // Create messageBox and set size constraints
                HBox messageBox = new HBox();
                messageBox.setAlignment(Pos.CENTER);

                messageBox.getStyleClass().add("messageConversationSearched");

                VBox vBox = new VBox();

                vBox.setPrefWidth(200);
                vBox.setMinWidth(200);
                vBox.setMaxWidth(200);
                vBox.setAlignment(Pos.CENTER);

                VBox content = new VBox();
                HBox authorBox = new HBox();
                Label infoLabel = new Label();
                infoLabel.setText(message.getSender().getDisplayName() + " - " + DateHandler.transformDate(message.getTime()));
                authorBox.getChildren().add(infoLabel);
                HBox labelBox = new HBox();
                Label label = new Label();
                label.getStyleClass().add("contentMessage");
                label.setText(message.getContent());

                content.getStyleClass().add("messageConversationSearchedBox");

                Region region = new Region();
                region.setMinHeight(10);

                labelBox.getChildren().addAll(label);
                content.getChildren().addAll(authorBox, labelBox);
                vBox.getChildren().addAll(region, content);

                messageBox.getChildren().add(vBox);

                // Ensure messageBox can grow horizontally
                messageBox.setMaxWidth(Double.MAX_VALUE);

                mainPageController.goToMessage(messageBox, conversation, message);
                // Add messageBox to messageList
                messageList.getChildren().add(messageBox);
            }
            scrollPane.setContent(messageList);
            rightSearchPane.getChildren().add(scrollPane);
        });
    }

    public void setSearchMessageField(VBox rightSearchPane, MainPageController mainPageController, Conversation conversation) {
        rightSearchPane.getChildren().clear();

        Region regionPane = new Region();
        regionPane.setMinHeight(20);
        TextField searchMessages = new TextField();
        searchMessages.setPromptText("Search for messages...");
        searchMessages.getStyleClass().add("searchMessages");

        mainPageController.searchMessageInConversation(rightSearchPane, searchMessages, conversation);

        rightSearchPane.setAlignment(Pos.TOP_CENTER);
        rightSearchPane.getChildren().addAll(regionPane, searchMessages);
    }
}
