package com.voicechat.test.mainpage.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.component.ConversationComponent;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.JsonMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class ConversationListComponentTest extends FxRobot {
    private ConversationListComponent conversationListComponent;
    private Conversation conversation;
    private User user;
    private Message message;
    @Mock
    private MainPageService mainPageService;
    @Mock
    private ServerReader serverReader;
    @Mock
    private PrintWriter out;

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

        conversationListComponent = new ConversationListComponent();

        Field mainPageServiceField = ConversationListComponent.class.getDeclaredField("mainPageService");
        mainPageServiceField.setAccessible(true);
        mainPageServiceField.set(conversationListComponent, mainPageService);

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
    void testAddLastMessageLabel() {
        VBox vBox = conversationListComponent.addLastMessageLabel(message);

        assertNotNull(vBox);
        assertFalse(vBox.getChildren().isEmpty());
    }

    @Test
    void testSetConversationList() throws Exception {
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setBinaryPayload(jsonMapper.writeValueAsBytes(List.of(conversation)));
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);

        VBox leftPane = new VBox();

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            conversationListComponent.setConversationList(new MainPageController(), leftPane, serverResponse);
            latch.countDown();
        });

        boolean scheduled = latch.await(5, TimeUnit.SECONDS);
        assertTrue(scheduled, "Timeout waiting for addMessageBox to be called");

        // Now, wait again for the UI updates to complete
        CountDownLatch updateLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Here, perform any additional checks or just signal completion
            updateLatch.countDown();
        });
        assertTrue(updateLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for UI update");

        assertNotNull(leftPane);
        assertFalse(leftPane.getChildren().isEmpty());
    }

    @Test
    void testCircleUnread() {
        Integer size = 15;
        StackPane stackPane = new StackPane();

        conversationListComponent.circleUnread(size, stackPane);

        assertNotNull(stackPane);
        assertFalse(stackPane.getChildren().isEmpty());
    }
}
