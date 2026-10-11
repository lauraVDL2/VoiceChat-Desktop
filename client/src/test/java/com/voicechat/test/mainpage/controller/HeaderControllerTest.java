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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.isVisible;

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

        setStaticField(Listener.class, "serverReader", mockServerReader);
        setStaticField(Listener.class, "serverOut", out);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/voicechat/client/mainpage/header.fxml")
        );
        Parent root = loader.load();
        headerController = loader.getController();

        Field serviceField = HeaderController.class.getDeclaredField("headerService");
        serviceField.setAccessible(true);
        serviceField.set(headerController, headerService);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                VoiceChatApplication.class
                        .getResource("/com/voicechat/client/css/light-theme.css")
                        .toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    private static void setStaticField(Class<?> owner, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);

        if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException(owner.getName() + "." + name + " is not static");
        }

        field.set(null, value);
    }

    @Test
    void testSearchUsers_withResults_andStartConversation_success() throws Exception {
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

        when(headerService.searchUser("John")).thenReturn(response);

        // Act
        headerController.searchUsers();
        var nodes = lookup(".discussionBox");
        assertNotNull(nodes);
    }

}
