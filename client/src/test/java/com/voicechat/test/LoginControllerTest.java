package com.voicechat.test;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.login.controller.LoginController;
import com.voicechat.client.login.service.LoginService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.shared.ServerResponse;
import org.shared.ServerResponseStatus;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.*;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest extends FxRobot {
    private LoginController controller;
    @Mock
    private LoginService mockLoginService;
    @Mock
    private ServerReader mockServerReader;
    @Mock
    private PrintWriter out;
    private javafx.scene.layout.StackPane rootPane;

    @Start
    public void start(Stage stage) throws Exception {
        // Load your FXML and set up the scene
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/login/login-view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        MockitoAnnotations.openMocks(this);

        // Inject mocks via reflection
        Field loginServiceField = LoginController.class.getDeclaredField("loginService");
        loginServiceField.setAccessible(true);
        loginServiceField.set(controller, mockLoginService);

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, mockServerReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        // Set scene and show stage
        stage.setScene(new Scene(root));
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        // Prepare mock response

        // Mock loginService.login to do nothing (simulate async)
        doNothing().when(mockLoginService).login(any(User.class), anyString());

        // Prepare a success response
        ServerResponse successResponse = new ServerResponse();
        successResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        // Mock getServerResponseByCorrelationId to return the success response
        when(mockServerReader.getServerResponseByCorrelationId(anyString()))
                .thenReturn(successResponse);
        when(mockServerReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());

        // Enter email and password
        clickOn("#emailAddressField").write("test@example.com");
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#passwordField").write("password");
        WaitForAsyncUtils.waitForFxEvents();

        // Click login button
        clickOn("#loginButton");

        // Wait for async and FX updates
        WaitForAsyncUtils.waitForFxEvents();

        // If login successful : no errorMessageLog label anymore
        Node errorMessageNode = lookup("#errorMessageLog").queryAll()
                .stream().findFirst().orElse(null);
        assertNull(errorMessageNode);
    }

    @Test
    void testLoginFailureDisplaysErrorMessage() throws Exception {
        // Mock login service to do nothing
        doNothing().when(mockLoginService).login(any(User.class), anyString());

        ServerResponse failureResponse = new ServerResponse();
        failureResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
        failureResponse.setMessage("Invalid credentials");
        when(mockServerReader.getServerResponseByCorrelationId(anyString())).thenReturn(failureResponse);

        // User input and click
        clickOn("#emailAddressField").write("wrong@example.com");
        clickOn("#passwordField").write("wrongpassword");
        clickOn("#loginButton");

        WaitForAsyncUtils.waitForFxEvents();

        // Assert the error message is visible and contains expected text
        verifyThat("#errorMessageLog", (Label label) ->
                label.isVisible() && "Invalid credentials".equals(label.getText()));
    }

}
