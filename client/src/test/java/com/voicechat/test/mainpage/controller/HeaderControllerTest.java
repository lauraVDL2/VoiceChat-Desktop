package com.voicechat.test.mainpage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.mainpage.controller.HeaderController;
import com.voicechat.client.mainpage.service.HeaderService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class HeaderControllerTest extends FxRobot {
    private HeaderController headerController;
    @Mock
    private HeaderService headerService;
    @Mock
    private ServerReader mockServerReader;
    @Mock
    private PrintWriter out;

    @Start
    public void start(Stage stage) throws Exception {
        MockitoAnnotations.openMocks(this);
        // Load FXML and set controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/mainpage/header.fxml"));
        Parent root = loader.load();
        headerController = loader.getController();

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, mockServerReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        Field  headerServiceField = HeaderController.class.getDeclaredField("headerService");
        headerServiceField.setAccessible(true);
        headerServiceField.set(headerController, headerService);

        // Show stage
        Scene scene = new Scene(root);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void testSearchUsers_withResults_andStartConversation() throws Exception {
        // Prepare mock response
        ServerResponse response = new ServerResponse();
        response.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        response.setServerResponseMessage(ServerResponseMessage.USER_SEARCHED);
        // Create dummy users
        User user1 = new User();
        user1.setDisplayName("John Doe");
        user1.setEmailAddress("john@example.com");
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        response.setBinaryPayload(objectMapper.writeValueAsBytes(List.of(user1)));

        // Mock getServerResponseByCorrelationId to return the success response
        when(mockServerReader.getServerResponseByCorrelationId(anyString()))
                .thenReturn(response);
        when(mockServerReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());
        when(headerService.searchUser(anyString())).thenReturn(response);

        // Simulate key press in searchField
        TextField searchField = lookup("#searchField").queryAs(TextField.class);
        clickOn(searchField).write("John");

        // Trigger key pressed event
        press(javafx.scene.input.KeyCode.ENTER).release(javafx.scene.input.KeyCode.ENTER);

        WaitForAsyncUtils.waitForFxEvents();

        // Verify UI update
        HBox hbox = lookup(".searchLabel").queryAs(HBox.class);
        assertNotNull(hbox);
        assertFalse(hbox.getChildren().isEmpty());
        Label label = (Label) hbox.getChildren().getFirst();
        assertTrue(label.getText().contains("John Doe"));

        response = new ServerResponse();
        response.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        response.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);

        when(mockServerReader.getServerResponseByCorrelationId(anyString()))
                .thenReturn(response);
        when(mockServerReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());
        when(headerService.searchConversationIfExists(any())).thenReturn(response);

        clickOn(hbox);
        WaitForAsyncUtils.waitForFxEvents();

        var nodes = lookup(".discussionBox");
        assertNotNull(nodes);
    }

}
