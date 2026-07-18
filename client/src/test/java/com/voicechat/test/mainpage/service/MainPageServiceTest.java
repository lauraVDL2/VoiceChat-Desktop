package com.voicechat.test.mainpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.mainpage.service.HeaderService;
import com.voicechat.client.mainpage.service.MainPageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.shared.JsonMapper;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;
import org.shared.pojo.Page;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MainPageServiceTest {
    private MainPageService mainPageService;
    private PrintWriter printWriter;
    private String correlationId = "test-correlation-id";
    private MockedStatic<Listener> listenerMock;
    private User user;
    private Message message;
    private Conversation conversation;
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
        conversation.setId(1L);
        conversation.setParticipants(Set.of(user));

        List<Conversation> conversationList = new ArrayList<>();
        conversationList.add(conversation);
        List<Message> messageList = new ArrayList<>();
        messageList.add(message);

        conversation.setMessages(messageList);
        user.setConversation(conversationList);
        user.setMessages(messageList);

        UserSession.INSTANCE.setUser(user);

        mainPageService = new MainPageService();
    }

    @Test
    void testCreateConversation_writesCorrectJsonMessage() throws Exception {
        mainPageService.createConversation(conversation);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.CONVERSATION_CREATE, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }

    @Test
    void testScrollMessages_writesCorrectJsonMessage() throws Exception {
        mainPageService.scrollMessages(conversation, new Page());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.CONVERSATION_SCROLL, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }

    @Test
    void testDisplayUserConversations_writesCorrectJsonMessage() throws Exception {
        mainPageService.displayUserConversations(user, correlationId);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.CONVERSATION_DISPLAY, message.getMessageType());

        User readUser = mapper.readValue(message.getPayload(), User.class);

        assertEquals(readUser.getEmailAddress(), user.getEmailAddress());
    }

    @Test
    void testSendAvatarInfo_writesCorrectJsonMessage() throws Exception {
        mainPageService.sendAvatarInfo(correlationId, user);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.READ_TARGET_AVATAR, message.getMessageType());

        User readUser = mapper.readValue(message.getPayload(), User.class);

        assertEquals(readUser.getEmailAddress(), user.getEmailAddress());
    }


    @Test
    void testGetConversation_writesCorrectJsonMessage() throws Exception {
        mainPageService.getConversation(conversation);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.CONVERSATION_GET, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }

    @Test
    void testSendMessage_writesCorrectJsonMessage() throws Exception {
        mainPageService.sendMessage(correlationId, conversation);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.MESSAGE_SEND, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }

    @Test
    void testSearchMessageInConversation_writesCorrectJsonMessage() throws Exception {
        mainPageService.searchMessageInConversation(conversation);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.MESSAGE_CONVERSATION_SEARCH, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }

    @Test
    void testGoToMessageInConversation_writesCorrectJsonMessage() throws Exception {
        mainPageService.goToMessageInConversation(conversation);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.MESSAGE_CONVERSATION_GO, message.getMessageType());

        Conversation readConversation = mapper.readValue(message.getPayload(), Conversation.class);

        assertEquals(readConversation.getId(), conversation.getId());
    }


    @AfterEach
    public void tearDown() {
        listenerMock.close();
    }

}
