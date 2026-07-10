package org.shared.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.action.UserNotificationAction;
import org.server.dao.ConversationDao;
import org.server.dao.UserDao;
import org.shared.*;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class UserNotificationActionTest {

    User sender;

    User target;

    Message entityMessage;

    @BeforeEach
    void setUp() {
        // create sender user (the message sender) and target user (to be notified)
        sender = new User();
        sender.setEmailAddress("sender@example.com");
        sender.setAvatar(null); // ensure no avatar bytes are sent

        target = new User();
        target.setEmailAddress("target@example.com");

        // build entity message (conversation messages)
        entityMessage = new Message();
        entityMessage.setTime(LocalDateTime.now());
        entityMessage.setContent("hello target");
        entityMessage.setSender(sender);
    }

    @Test
    void sendMessageToUser_success() throws Exception {
        UserDao userDao = mock(UserDao.class);
        ConversationDao conversationDao = mock(ConversationDao.class);
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        ServerResponse serverResponse = new ServerResponse();
        UserNotificationAction userNotificationAction = new UserNotificationAction(userDao, conversationDao);

        Conversation conversation = createDummyConversation();

        // build org.shared.Message object (control/correlation id)
        org.shared.Message messageObj = new org.shared.Message(MessageType.MESSAGE_SEND,
                objectMapper.writeValueAsString(conversation));
        messageObj.setCorrelationId("corr-42");

        // prepare sockets map: target socket should receive notification
        ConcurrentHashMap<String, Socket> userSockets = new ConcurrentHashMap<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket senderSocket = mock(Socket.class);
        Socket targetSocket = mock(Socket.class);
        userSockets.put(sender.getEmailAddress(), senderSocket);
        userSockets.put(target.getEmailAddress(), targetSocket);
        when(targetSocket.getOutputStream()).thenReturn(baos);

        when(conversationDao.getConversationParticipants(any(Conversation.class))).thenReturn(Set.of(sender, target));
        when(userDao.findUserByEmailAddress(any(String.class))).thenReturn(sender);

        // Act
        userNotificationAction.sendMessageToUser(messageObj, userSockets, objectMapper, serverResponse, conversation);

        // Assert serverResponse modifications
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.NEW_MESSAGE_NOTIFIED, serverResponse.getServerResponseMessage());
        // correlation id should be prefixed with "notif-"
        assertEquals("notif-corr-42", serverResponse.getCorrelationId());
        // userMessageMap should contain the notif key
        assertTrue(serverResponse.getUserMessageMap().containsKey("notif-" + target.getEmailAddress()));
        assertEquals("notif-" + messageObj.getCorrelationId(),
                serverResponse.getUserMessageMap().get("notif-" + target.getEmailAddress()));

        // Verify bytes written to socket
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);

        // Deserialize server response that was sent over the socket
        ServerResponse payloadResponse = objectMapper.readValue(payload, ServerResponse.class);
        assertEquals("notif-corr-42", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.NEW_MESSAGE_NOTIFIED, payloadResponse.getServerResponseMessage());

        // The binary payload should contain the conversation object
        byte[] inner = payloadResponse.getBinaryPayload();
        Conversation writtenConversation = objectMapper.readValue(inner, Conversation.class);
        assertEquals(conversation.getId(), writtenConversation.getId());
        // messages should be present and match
        assertNotNull(writtenConversation.getMessages());
        assertEquals(1, writtenConversation.getMessages().size());
        assertEquals(entityMessage.getContent(), writtenConversation.getMessages().getFirst().getContent());
    }

    private Conversation createDummyConversation() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setMessages(List.of(entityMessage));

        return conversation;
    }
}
