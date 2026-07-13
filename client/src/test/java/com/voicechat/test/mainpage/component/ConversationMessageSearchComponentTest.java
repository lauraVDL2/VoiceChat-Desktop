package com.voicechat.test.mainpage.component;

import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.component.ConversationListComponent;
import com.voicechat.client.mainpage.component.ConversationMessageSearchComponent;
import com.voicechat.client.mainpage.controller.MainPageController;
import com.voicechat.client.mainpage.service.MainPageService;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class ConversationMessageSearchComponentTest extends FxRobot {
    private ConversationMessageSearchComponent conversationMessageSearchComponent;
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

        conversationMessageSearchComponent = new ConversationMessageSearchComponent();

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
    void testSetMessagesSearched() throws Exception {
        VBox rightSearchPane = new VBox();

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            conversationMessageSearchComponent.setMessagesSearched(rightSearchPane, List.of(message),
                    conversation, new MainPageController());
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

        assertNotNull(rightSearchPane);
        assertFalse(rightSearchPane.getChildren().isEmpty());
    }

    @Test
    void testSearchMessageField() {
        VBox rightSearchPane = new VBox();

        conversationMessageSearchComponent.setSearchMessageField(rightSearchPane,
                new MainPageController(), conversation);

        assertNotNull(rightSearchPane);
        assertFalse(rightSearchPane.getChildren().isEmpty());
    }
}
