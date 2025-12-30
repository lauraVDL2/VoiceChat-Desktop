package com.voicechat.client.common.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.common.UserSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.entity.User;

import java.io.PrintWriter;

public class TopRightWindowComponent {

    public BorderPane initWindowComponent(BorderPane borderPane) {
        HBox hBox = new HBox();
        ImageView imageViewClose = new ImageView();
        Image imageClose = new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/close.png"));
        imageViewClose.setImage(imageClose);
        imageViewClose.setFitWidth(20);
        imageViewClose.setFitHeight(20);

        imageViewClose.setOnMouseClicked(e -> {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.close();
            exit();
        });

        ImageView imageViewMinimize = new ImageView();
        Image imageMinimize = new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/minimize.png"));
        imageViewMinimize.setImage(imageMinimize);
        imageViewMinimize.setFitWidth(20);
        imageViewMinimize.setFitHeight(20);

        imageViewMinimize.setOnMouseClicked(e2 -> {
            Stage stage = (Stage) ((Node) e2.getSource()).getScene().getWindow();
            stage.setIconified(true);
        });

        ImageView imageViewMaximize = new ImageView();
        Image imageMaximize = new Image(VoiceChatApplication.class.getResourceAsStream("/com/voicechat/client/images/maximize.png"));
        imageViewMaximize.setImage(imageMaximize);
        imageViewMaximize.setFitWidth(20);
        imageViewMaximize.setFitHeight(20);

        imageViewMaximize.setOnMouseClicked(e3 -> {
            Stage stage = (Stage) ((Node) e3.getSource()).getScene().getWindow();
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });

        hBox.setAlignment(Pos.CENTER_RIGHT);
        Region margin1 = new Region();
        margin1.setPrefWidth(16);
        Region margin2 = new Region();
        margin2.setPrefWidth(16);
        hBox.getChildren().addAll(imageViewMinimize, margin1, imageViewMaximize, margin2, imageViewClose);

        borderPane.setTop(hBox);
        BorderPane.setAlignment(hBox, Pos.TOP_RIGHT);
        BorderPane.setMargin(hBox, new Insets(10));
        return borderPane;
    }

    public void exit() {
        try {
            closeSession();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }

    public void closeSession() throws JsonProcessingException {
        User user = UserSession.INSTANCE.getUser();
        if (user != null) {
            ObjectMapper objectMapper = JsonMapper.getJsonMapper();
            String json = objectMapper.writeValueAsString(user);
            Message message = new Message(MessageType.USER_EXIT, json);
            PrintWriter serverOut = Listener.getServerOut();

            serverOut.println(objectMapper.writeValueAsString(message));

            UserSession.INSTANCE.clear();
        }
    }
}
