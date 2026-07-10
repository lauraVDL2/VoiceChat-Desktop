package org.shared.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.server.dao.MessageDao;
import org.server.action.MessageAction;
import org.server.dao.MessageDaoImpl;
import org.shared.JsonMapper;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;

import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class MessageActionTest {

    @Test
    void sendMessage_success_writesJsonResponse() throws Exception {
        // create a mock DAO and inject into the action
        MessageDao messageDao = mock(MessageDao.class);
        MessageAction action = new MessageAction(messageDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        // Create objects used by the method
        ServerResponse serverResponse = new ServerResponse(); // real POJO to inspect changes
        Conversation conversationFromPayload = createDummyConversation();

        User user = new User();
        user.setEmailAddress("test.test@yahoo.fr");
        Message persistedMessage = createDummyMessage("second message", user);
        conversationFromPayload.getParticipants().add(user);
        when(messageDao.sendMessage(any(Conversation.class))).thenReturn(persistedMessage);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        // Provide correlation id and payload on messageObj
        org.shared.Message messageObj = new org.shared.Message(MessageType.MESSAGE_SEND,
                jsonMapper.writeValueAsString(conversationFromPayload));
        messageObj.setCorrelationId("corr-1");

        // Act
        Conversation result = action.sendMessage(jsonMapper, messageObj, serverResponse, socket);

        // Assert return value
        assertNotNull(result);
        // method sets messages list containing the message
        assertNotNull(result.getMessages());
        assertEquals(1, result.getMessages().size());
        assertSame(persistedMessage, result.getMessages().getFirst());

        // Assert serverResponse fields were set by the method
        assertEquals("corr-1", serverResponse.getCorrelationId());
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
        assertEquals("corr-1", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        // binary payload should contain the persisted message
        byte[] inner = payloadResponse.getBinaryPayload();
        Message writtenMessage = jsonMapper.readValue(inner, Message.class);
        assertEquals(persistedMessage.getContent(), writtenMessage.getContent());
        // Verify interaction with the injected DAO mock (called with a Conversation instance)
    }

    @Test
    void searchMessageInConversation_success_writesJsonResponse() throws Exception {
        MessageDao messageDao = mock(MessageDao.class);
        MessageAction action = new MessageAction(messageDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();
        Conversation conversationFromPayload = createDummyConversation();

        List<Message> foundMessages = List.of(createDummyMessage("found message", new User()));
        when(messageDao.searchMessageInConversation(any(Conversation.class))).thenReturn(foundMessages);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        org.shared.Message messageObj = new org.shared.Message(MessageType.MESSAGE_CONVERSATION_SEARCH,
                jsonMapper.writeValueAsString(conversationFromPayload));
        messageObj.setCorrelationId("corr-2");

        action.searchMessageInConversation(jsonMapper, messageObj, serverResponse, socket);

        assertEquals("corr-2", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());

        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("corr-2", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
    }

    private Message createDummyMessage(String content, User user) {
        Message message = new Message();
        String str = "2014-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        message.setTime(dateTime);
        message.setContent(content);
        var statuses = new ArrayList<ReadStatus>();
        statuses.add(new ReadStatus());
        statuses.add(new ReadStatus());
        message.setReadStatuses(statuses);
        message.setSender(user);
        return message;
    }

    private Conversation createDummyConversation() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        User user = new User();
        user.setId(1L);
        user.setEmailAddress("toto.toto@yahoo.fr");
        Message message = createDummyMessage("first message", user);
        conversation.setMessages(List.of(message));
        var set = new HashSet<User>();
        set.add(user);
        conversation.setParticipants(set);
        return conversation;
    }
}
