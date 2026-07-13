package com.voicechat.test.mainpage.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.component.ConversationAction;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class ConversationComponentTest extends FxRobot {
    private ConversationComponent conversationComponent;
    private Conversation conversation;
    private User user;
    private Message message;
    @Mock
    private MainPageService mainPageService;
    @Mock
    private ServerReader serverReader;
    @Mock
    private PrintWriter out;

//    @BeforeAll
//    public static void staticSetup() throws Exception {
//        Platform.startup(() -> {}); // Initialize JavaFX
//    }

    @BeforeEach
    public void setup() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Prepare sample data
        user = new User();
        user.setDisplayName("test");
        user.setPassword("password123");
        user.setEmailAddress("test.test@example.com");
        message = new Message();
        message.setContent("Hello");
        message.setTime(LocalDateTime.now());
        message.setSender(user);
        conversation = new Conversation();
        conversation.setParticipants(Set.of(user));

        List<Conversation> conversationList = new ArrayList<>();
        conversationList.add(conversation);
        List<Message> messageList = new ArrayList<>();
        messageList.add(message);

        conversation.setMessages(messageList);
        user.setConversation(conversationList);
        user.setMessages(messageList);

        UserSession.INSTANCE.setUser(user);

        conversationComponent = new ConversationComponent();

        Field mainPageServiceField = ConversationComponent.class.getDeclaredField("mainPageService");
        mainPageServiceField.setAccessible(true);
        mainPageServiceField.set(conversationComponent, mainPageService);

        Field serverReaderField = Listener.class.getDeclaredField("serverReader");
        serverReaderField.setAccessible(true);
        serverReaderField.set(null, serverReader);

        Field serverOutField = Listener.class.getDeclaredField("serverOut");
        serverOutField.setAccessible(true);
        serverOutField.set(null, out);

        when(serverReader.getAvatarByCorrelationId(anyString()))
                .thenReturn(new ImageView());
    }

    @Test
    void testSetConversationComponents_setsCenterHBox() throws Exception {
        // Create ServerResponse with payload
        ServerResponse response = new ServerResponse();
        response.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        response.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        response.setBinaryPayload(mapper.writeValueAsBytes(conversation));

        // Create mainPane
        BorderPane mainPane = new BorderPane();
        conversationComponent.setConversationComponents(mainPane, response);
        WaitForAsyncUtils.waitForFxEvents();

        // Verify
        assertNotNull(mainPane.getCenter());
        assertInstanceOf(HBox.class, mainPane.getCenter());
        HBox hbox = (HBox) mainPane.getCenter();
        assertFalse(hbox.getChildren().isEmpty());
    }

    @Test
    void testAddMessageComponents() throws Exception {
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        serverResponse.setBinaryPayload(mapper.writeValueAsBytes(message));

        BorderPane mainPane = new BorderPane();
        VBox vBox = new VBox();
        vBox.setId("messageContentBox");
        mainPane.getChildren().add(vBox);

        ConversationComponent spyConversationComponent = spy(conversationComponent);

        doReturn(new VBox()).when(spyConversationComponent)
                .addMessageBox(any(HBox.class), any(VBox.class), any(Message.class), any(User.class));

        doReturn(new ScrollPane()).when(spyConversationComponent)
                .addMessagesScrollPane(any(MainPageController.class), any(VBox.class), any(Conversation.class));

        spyConversationComponent.addMessageComponents(new MainPageController(), mainPane, serverResponse,
                conversation);

        WaitForAsyncUtils.waitForFxEvents();

        // Verify
        assertNotNull(mainPane.getCenter());
        assertInstanceOf(ScrollPane.class, mainPane.getCenter());
    }

    @Test
    void testAddMessageBox() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final VBox[] resultHolder = new VBox[1];

        Platform.runLater(() -> {
            HBox hBoxAvatar = new HBox();
            VBox messageContentBox = new VBox();

            resultHolder[0] = conversationComponent.addMessageBox(hBoxAvatar, messageContentBox, message, user);
            latch.countDown();
        });

        // Wait for the addMessageBox to schedule its UI update
        boolean scheduled = latch.await(5, TimeUnit.SECONDS);
        assertTrue(scheduled, "Timeout waiting for addMessageBox to be called");

        // Now, wait again for the UI updates to complete
        CountDownLatch updateLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Here, perform any additional checks or just signal completion
            updateLatch.countDown();
        });
        assertTrue(updateLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for UI update");

        VBox result = resultHolder[0];
        assertNotNull(result);
        assertFalse(result.getChildren().isEmpty());
    }

    @Test
    void testAddConversationTopBox() {
        Set<User> participants = Set.of(user, new User());

        HBox hbox = conversationComponent.addConversationTopBox(participants, user);

        assertNotNull(hbox);
        assertFalse(hbox.getChildren().isEmpty());
    }

    @Test
    void testAddConversationMessagesScrollPane() {
        GridPane gridPane = new GridPane();

        ConversationComponent spyConversationComponent = spy(conversationComponent);

        doReturn(new VBox()).when(spyConversationComponent)
                .addMessageBox(any(HBox.class), any(VBox.class), any(Message.class), any(User.class));

        ScrollPane scrollPane = spyConversationComponent.addConversationMessagesScrollPane(new MainPageController(),
                conversation, gridPane, user);

        assertNotNull(scrollPane);
        assertNotNull(scrollPane.getContent());
    }

    @Test
    void testAddConversationSendMessageBox() {
        ConversationAction conversationAction = ConversationAction.START;

        HBox hBox = conversationComponent.addConversationSendMessageBox(new MainPageController(),
                conversation, conversationAction);

        assertNotNull(hBox);
        assertFalse(hBox.getChildren().isEmpty());
    }

    @Test
    void testAddMessagesScrollPane() {
        VBox vBox = new VBox();

        ScrollPane scrollPane = conversationComponent.addMessagesScrollPane(new MainPageController(),
                vBox, conversation);

        assertNotNull(scrollPane);
        assertNotNull(scrollPane.getContent());
    }

}
