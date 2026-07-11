package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.dao.ConversationDao;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class ConversationActionTest {

    List<User> users = new ArrayList<>();
    Conversation conversation;

    @BeforeEach
    void setUp() {
        User user1 = new User();
        user1.setEmailAddress("toto.toto@domain.com");
        user1.setDisplayName("toto");
        user1.setUserName("toto");
        users.add(user1);
        User user2 = new User();
        user2.setEmailAddress("toto2.toto@domain.com");
        user2.setDisplayName("toto2");
        user2.setUserName("toto2");
        users.add(user2);
        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setMessages(List.of(new org.shared.entity.Message()));
        conversation.setParticipants(new HashSet<>(users));
    }

    @Test
    void getConversation_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.getConversation(any(Conversation.class))).thenReturn(conversation);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.CONVERSATION_GET,
                jsonMapper.writeValueAsString(conversation));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.getConversation(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        // binary payload should contain the persisted message
        byte[] inner = payloadResponse.getBinaryPayload();
        org.shared.entity.Conversation conversation1 = jsonMapper.readValue(inner, org.shared.entity.Conversation.class);
        assertEquals(conversation1.getId(), conversation.getId());
    }

    @Test
    void goToMessage_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.scrollConversationMessages(any(Conversation.class), anyInt())).thenReturn(conversation);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.MESSAGE_CONVERSATION_GO,
                jsonMapper.writeValueAsString(conversation));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.goToMessage(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }

    @Test
    void scrollConversation_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.scrollConversationMessages(any(Conversation.class), anyInt())).thenReturn(conversation);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.CONVERSATION_SCROLL,
                jsonMapper.writeValueAsString(conversation));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.scrollConversation(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }

    @Test
    void searchUserConversations_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.searchUserConversations(any(User.class))).thenReturn(List.of(conversation));

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.CONVERSATION_DISPLAY,
                jsonMapper.writeValueAsString(users.getFirst()));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.searchUserConversations(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }

    @Test
    void createConversation_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.createConversation(any(), any(), any()))
                .thenReturn(conversation);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.CONVERSATION_CREATE,
                jsonMapper.writeValueAsString(conversation));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.createConversation(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }

    @Test
    void conversationSearchIfExists_success_writesJsonResponse() throws Exception {
        ConversationDao conversationDao = mock(ConversationDao.class);
        ConversationAction conversationAction = new ConversationAction(conversationDao);
        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(conversationDao.searchConversationIfExists(any(User.class), any(User.class)))
                .thenReturn(conversation);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.CONVERSATION_SEARCH,
                jsonMapper.writeValueAsString(users));
        messageObj.setCorrelationId("test-correlation-id");

        conversationAction.conversationSearchIfExists(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse fields
        assertEquals("test-correlation-id", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        // Parse the bytes written to the socket (DataOutputStream wrote: writeUTF("JSON_RESPONSE"), writeInt(len), write(bytes))
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("test-correlation-id", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }
}
