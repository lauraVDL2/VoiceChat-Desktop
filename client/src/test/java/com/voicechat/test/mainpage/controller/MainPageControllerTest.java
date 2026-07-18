package com.voicechat.test.mainpage.controller;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.VoiceChatApplication;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.controller.HeaderController;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.pojo.Page;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class MainPageControllerTest extends FxRobot {
    private MainPageController mainPageController;
    @Mock
    private MainPageService mainPageService;
    @Mock
    private ServerReader serverReader;
    @Mock
    private HeaderController headerController;
    @Mock
    private ConversationComponent conversationComponent;
    @Mock
    private ConversationListComponent conversationListComponent;
    @Mock
    private PrintWriter out;
    private String correlationId = "corr-123";

    @Start
    public void start(Stage stage) throws Exception {
        MockitoAnnotations.openMocks(this);
        // Load FXML and set controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicechat/client/mainpage/main-page-view.fxml"));
        Parent root = loader.load();
        mainPageController = loader.getController();
        when(serverReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, serverReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        Field headerServiceField = MainPageController.class.getDeclaredField("mainPageService");
        headerServiceField.setAccessible(true);
        headerServiceField.set(mainPageController, mainPageService);

        Field conversationComponentField = MainPageController.class.getDeclaredField("conversationComponent");
        conversationComponentField.setAccessible(true);
        conversationComponentField.set(mainPageController, conversationComponent);

        Field conversationListComponentField = MainPageController.class.getDeclaredField("conversationListComponent");
        conversationListComponentField.setAccessible(true);
        conversationListComponentField.set(mainPageController, conversationListComponent);

        // Show stage
        Scene scene = new Scene(root);
        scene.getStylesheets().add(VoiceChatApplication.class.getResource("/com/voicechat/client/css/light-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    @Test
    void testGoToMessage_successfulResponse() throws Exception {
        // Arrange
        HBox mockHBox = Mockito.mock(HBox.class);
        Conversation conversation = new Conversation();
        Message message = new Message();
        // Mock mainPageService response
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_CONVERSATION_WENT);
        serverResponse.setPage(new Page());
        // Mock JSON payload
        Conversation convFromPayload = new Conversation();
        byte[] payloadBytes = JsonMapper.getJsonMapper().writeValueAsBytes(convFromPayload);
        serverResponse.setBinaryPayload(payloadBytes);

        when(serverReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());

        when(mainPageService.goToMessageInConversation(any())).thenReturn(serverResponse);
        // Act
        mainPageController.goToMessage(mockHBox, conversation, message);

        // Simulate mouse click event
        verify(mockHBox).setOnMouseClicked(any());
    }

    /*@Test
    void testGetUserConversations_success() throws Exception {
        // Arrange
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_DISPLAYED);
        // Mock conversation list response
        when(Listener.getServerReader().getServerResponseByCorrelationId(anyString())).thenReturn(serverResponse);

        // Spy on mainPageController to verify setConversationList
        MainPageController spyController = Mockito.spy(mainPageController);

        // Act
        spyController.getUserConversations();

        // Verify setConversationList is called
        verify(conversationListComponent).setConversationList(any(), any(), any());
    }*/

    @Test
    void testSendMessageToExistingConversation_success() throws Exception {
        // Arrange
        ImageView mockImageView = Mockito.mock(ImageView.class);
        Conversation conversation = new Conversation();
        conversation.setId(123L);
        ImageView imageView = Mockito.mock(ImageView.class);

        MainPageController spyController = Mockito.spy(mainPageController);
        BorderPane borderPane = Mockito.mock(BorderPane.class);

        when(spyController.getMainPane()).thenReturn(borderPane);
        when(spyController.getMainPane().lookup("#sendMessage")).thenReturn(imageView);

        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_SENT);

        when(mainPageService.sendMessage(any(), any())).thenReturn(serverResponse);

        // Act
        spyController.sendMessageToExistingConversation(mockImageView, conversation);

        // Verify that setOnMouseClicked is set
        verify(mockImageView).setOnMouseClicked(any());
    }

    @Test
    void testSearchMessageInConversation_success() throws Exception {
        // Arrange
        TextField mockSearchField = Mockito.mock(TextField.class);
        when(mockSearchField.getText()).thenReturn("searchTerm");

        Conversation conversation = new Conversation();

        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_CONVERSATION_SEARCHED);
        List<Message> messages = List.of(new Message());
        byte[] payload = JsonMapper.getJsonMapper().writeValueAsBytes(messages);
        serverResponse.setBinaryPayload(payload);

        when(mainPageService.searchMessageInConversation(any())).thenReturn(serverResponse);

        // Act
        mainPageController.searchMessageInConversation(Mockito.mock(VBox.class), mockSearchField, conversation);

        // Simulate action event
        verify(mockSearchField).setOnAction(any());
        var nodes = lookup(".contentMessage");
        assertNotNull(nodes);
    }

    @Test
    void testGoToConversation_success() throws Exception {
        // Arrange
        StackPane mockStackPane = Mockito.mock(StackPane.class);
        Node mockNode = Mockito.mock(Node.class);
        VBox mockVBox = Mockito.mock(VBox.class);
        // Setup getChildren().getFirst()
        when(mockVBox.getId()).thenReturn("c123");
        when(mockStackPane.getStyleClass()).thenReturn(FXCollections.observableArrayList());

        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_GET);
        // Mock getConversation response
        when(mainPageService.getConversation(any())).thenReturn(serverResponse);

        // Act
        // Simulate event
        mainPageController.goToConversation(mockStackPane);

        var nodes = lookup(".contentMessage");
        assertNotNull(nodes);
    }

}
