package com.voicechat.test.mainpage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.mainpage.service.HeaderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.User;

import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HeaderServiceTest {
    private HeaderService headerService;
    private PrintWriter printWriter;
    private String correlationId = "test-correlation-id";
    private MockedStatic<Listener> listenerMock;
    private User user;
    private ServerReader serverReader;

    @BeforeEach
    public void setup() throws Exception {
        // Setup mocks
        printWriter = mock(PrintWriter.class);
        listenerMock = Mockito.mockStatic(Listener.class);
        serverReader = Mockito.mock(ServerReader.class);

        listenerMock.when(Listener::getServerOut).thenReturn(printWriter);
        listenerMock.when(Listener::getServerReader).thenReturn(serverReader);
        Mockito.when(serverReader.getServerResponseByCorrelationId(Mockito.anyString()))
                .thenReturn(new ServerResponse());


        user = new User();
        user.setUserName("test");
        user.setDisplayName("test");
        user.setPassword("password123");
        user.setEmailAddress("test.test@example.com");

        headerService = new HeaderService();
    }

    @Test
    void searchUser_writesCorrectJsonToServer() throws Exception {
        headerService.searchUser("test");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Message message = mapper.readValue(sentJson, Message.class);

        // Verify message fields
        assertSame(MessageType.USER_SEARCH, message.getMessageType());

        User readUser = mapper.readValue(message.getPayload(), User.class);

        assertEquals(readUser.getDisplayName(), user.getDisplayName());
    }

    @Test
    void searchConversationIfExists_writesCorrectJsonToServer() throws Exception {
        headerService.searchConversationIfExists(List.of(user, new User()));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Message message = mapper.readValue(sentJson, Message.class);

        // Verify message fields
        assertSame(MessageType.CONVERSATION_SEARCH, message.getMessageType());

        List<User> readUser = mapper.readValue(message.getPayload(), new TypeReference<List<User>>() {});

        assertNotNull(readUser);
        assertEquals(2, readUser.size());
    }

    @Test
    void sendAvatarInfo_writesCorrectJsonToServer() throws Exception {
        headerService.sendAvatarInfo(correlationId, user);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Message message = mapper.readValue(sentJson, Message.class);

        // Verify message fields
        assertSame(MessageType.READ_TARGET_AVATAR, message.getMessageType());
        assertEquals(message.getCorrelationId(), correlationId);

        User targetUser = mapper.readValue(message.getPayload(), User.class);

        assertNotNull(targetUser);
        assertEquals(targetUser.getDisplayName(), user.getDisplayName());
    }

    @AfterEach
    public void tearDown() {
        listenerMock.close();
    }
}
