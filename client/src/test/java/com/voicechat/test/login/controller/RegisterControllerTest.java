package com.voicechat.test.login.controller;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.login.controller.RegisterController;
import com.voicechat.client.login.service.RegisterService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class RegisterControllerTest extends FxRobot {
    private RegisterController registerController;
    @Mock
    private RegisterService registerService;
    @Mock
    private ServerReader mockServerReader;
    @Mock
    private PrintWriter out;

    @Start
    public void start(Stage stage) throws Exception {
        // Open mocks first
        MockitoAnnotations.openMocks(this);

        // Inject static Listener fields BEFORE loading the FXML so the controller's
        // constructor/initialize() sees the mocks
        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, mockServerReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        // Now load the FXML (initialize() will run and use the mocked serverReader)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/login/register-view.fxml"));
        Parent root = loader.load();
        registerController = loader.getController();

        // Inject controller instance fields (registerService) if needed
        Field registerServiceField = RegisterController.class.getDeclaredField("registerService");
        registerServiceField.setAccessible(true);
        registerServiceField.set(registerController, registerService);

        // Show stage
        stage.setScene(new Scene(root));
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void testRegisterUser_successful() throws Exception {
        doNothing().when(registerService).register(any(User.class), anyString());
        when(registerService.verifyEmail(anyString())).thenReturn(true);

        // Prepare a success response
        ServerResponse successResponse = new ServerResponse();
        successResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        // Mock getServerResponseByCorrelationId to return the success response
        when(mockServerReader.getServerResponseByCorrelationId(anyString()))
                .thenReturn(successResponse);
        when(mockServerReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());

        // Enter email and password
        clickOn("#emailAddressField").write("test.test@example.com");
        clickOn("#displayedNameField").write("test");
        clickOn("#passwordField").write("password");

        // Click login button
        clickOn("#registerButton");

        // Wait for async and FX updates
        WaitForAsyncUtils.waitForFxEvents();

        // If register successful : no errorMessageLog label anymore
        Node errorMessageNode = lookup("#errorMessageLog").queryAll()
                .stream().findFirst().orElse(null);
        assertNull(errorMessageNode);
    }

    @Test
    void testRegisterUser_alreadyExists() throws Exception {
        doNothing().when(registerService).register(any(User.class), anyString());

        // Prepare a response
        ServerResponse failureResponse = new ServerResponse();
        failureResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
        failureResponse.setServerResponseMessage(ServerResponseMessage.USER_CREATED);
        failureResponse.setMessage("User already exists !");


        // Mock getServerResponseByCorrelationId to return the success response
        when(mockServerReader.getServerResponseByCorrelationId(anyString()))
                .thenReturn(failureResponse);
        when(registerService.verifyEmail(anyString())).thenReturn(true);

        // Enter email and password
        clickOn("#emailAddressField").write("test.test@example.com");
        clickOn("#displayedNameField").write("test");
        clickOn("#passwordField").write("password");
        // Click login button
        clickOn("#registerButton");

        // Wait for async and FX updates
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#errorMessageLog", (Label label) ->
                label.isVisible() && "User already exists !".equals(label.getText()));
    }

}
