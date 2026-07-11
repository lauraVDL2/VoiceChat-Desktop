package org.server.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.action.UserAction;
import org.server.dao.UserDao;
import org.shared.*;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserActionTest {

    User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmailAddress("test@example.com");
        user.setId(1L);
        user.setUserName("TEST TEST");
        user.setDisplayName("TEST TEST");
        user.setAvatar("/images/avatar/avatar_default.png");
    }

    @Test
    void userCreate8success_writesJsonResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = new UserAction(userDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();
        when(userDao.saveUser(any(User.class))).thenReturn(true);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.USER_CREATE,
                jsonMapper.writeValueAsString(user));
        messageObj.setCorrelationId("corr-1");

        // Act
        User result = action.userCreate(jsonMapper, messageObj, serverResponse, socket);

        // Assert return value
        assertNotNull(result);
        assertNotNull(result.getEmailAddress());
        assertEquals(result.getEmailAddress(), user.getEmailAddress());

        // Assert serverResponse
        assertEquals(serverResponse.getCorrelationId(), messageObj.getCorrelationId());
        assertEquals(serverResponse.getServerResponseStatus(), org.shared.ServerResponseStatus.SUCCESS);

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
    }

    @Test
    void userLogin_success_writesJsonResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = new UserAction(userDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();
        when(userDao.login(any(User.class))).thenReturn(user);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.USER_LOG_IN,
                jsonMapper.writeValueAsString(user));
        messageObj.setCorrelationId("corr-2");

        // Act
        User result = action.userLogIn(jsonMapper, messageObj, serverResponse, socket);

        // Assert return value
        assertNotNull(result);
        assertNotNull(result.getEmailAddress());
        assertEquals(result.getEmailAddress(), user.getEmailAddress());

        // Assert serverResponse
        assertEquals(serverResponse.getCorrelationId(), messageObj.getCorrelationId());
        assertEquals(serverResponse.getServerResponseStatus(), org.shared.ServerResponseStatus.SUCCESS);

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
        assertEquals("corr-2", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());

        // binary payload should contain the persisted message
        byte[] inner = payloadResponse.getBinaryPayload();
        org.shared.entity.User loggedUser = jsonMapper.readValue(inner, org.shared.entity.User.class);
        assertEquals(user.getEmailAddress(), loggedUser.getEmailAddress());
    }

    @Test
    void userSearch_success_writesJsonResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = new UserAction(userDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();
        when(userDao.searchUsers(any(String.class))).thenReturn(List.of(user));

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.USER_SEARCH,
                jsonMapper.writeValueAsString(user));
        messageObj.setCorrelationId("corr-3");

        // Act
        action.userSearch(jsonMapper, messageObj, serverResponse, socket);

        // Assert serverResponse
        assertEquals(serverResponse.getCorrelationId(), messageObj.getCorrelationId());
        assertEquals(serverResponse.getServerResponseStatus(), org.shared.ServerResponseStatus.SUCCESS);

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
        assertEquals("corr-3", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        // binary payload should contain the persisted message
        byte[] inner = payloadResponse.getBinaryPayload();
        List<org.shared.entity.User> searchedUsers = jsonMapper.readValue(inner, new TypeReference<List<org.shared.entity.User>>() {});
        assertEquals(user.getEmailAddress(), searchedUsers.getFirst().getEmailAddress());
    }

    @Test
    void getOnlineUsers_success_writesJsonResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = new UserAction(userDao);
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create mock online users map
        ConcurrentHashMap<String, UserSessionStatus> onlineUsers = new ConcurrentHashMap<>();
        onlineUsers.put("user1", UserSessionStatus.ONLINE);
        onlineUsers.put("user2", UserSessionStatus.ONLINE);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.ONLINE_USERS_FETCH, "");
        messageObj.setCorrelationId("corr-4");

        // Act
        action.getOnlineUsers(messageObj, jsonMapper, serverResponse, socket, onlineUsers);

        // Assert serverResponse
        assertEquals(serverResponse.getCorrelationId(), messageObj.getCorrelationId());
        assertEquals(serverResponse.getServerResponseStatus(), ServerResponseStatus.SUCCESS);
        assertEquals(serverResponse.getServerResponseMessage(), ServerResponseMessage.ONLINE_USERS_FETCHED);

        // Parse the bytes written to the socket
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);

        // Deserialize the server response written to the socket and assert fields
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("corr-4", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());

        // Assert server information contains the online users
        assertNotNull(payloadResponse.getServerInformation());
        assertEquals(2, payloadResponse.getServerInformation().getOnlineUsers().size());
        assertTrue(payloadResponse.getServerInformation().getOnlineUsers().containsKey("user1"));
        assertTrue(payloadResponse.getServerInformation().getOnlineUsers().containsKey("user2"));
    }

    @Test
    void searchTargetUser_success_writesImageResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = spy(new UserAction(userDao));
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        Message messageObj = new Message(MessageType.READ_TARGET_AVATAR,
                jsonMapper.writeValueAsString(user));
        messageObj.setCorrelationId("corr-5");

        // Capture DataOutputStream output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(baos);

        // Mock the getAvatarBytes method to return dummy data
        byte[] mockAvatarBytes = new byte[]{1, 2, 3, 4, 5};
        doReturn(mockAvatarBytes).when(action).getAvatarBytes(user.getAvatar());

        // Act
        action.searchTargetUser(jsonMapper, messageObj, dataOutputStream, serverResponse);

        // Parse the bytes written to the DataOutputStream
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("IMAGE_RESPONSE", tag);

        // Deserialize the server response
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("corr-5", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.READ_TARGET_AVATAR, payloadResponse.getServerResponseMessage());
        assertArrayEquals(mockAvatarBytes, payloadResponse.getBinaryPayload());
    }

    @Test
    void searchUserAvatar_success_writesImageResponse() throws Exception {
        UserDao userDao = mock(UserDao.class);
        UserAction action = spy(new UserAction(userDao));
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create user with avatar

        Message messageObj = new Message(MessageType.USER_LOG_IN, "");
        messageObj.setCorrelationId("corr-6");

        // Capture DataOutputStream output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(baos);

        // Mock the getAvatarBytes method to return dummy data
        byte[] mockAvatarBytes = new byte[]{1, 2, 3, 4, 5};
        doReturn(mockAvatarBytes).when(action).getAvatarBytes(user.getAvatar());

        // Act
        action.searchUserAvatar(messageObj, jsonMapper, user, dataOutputStream, serverResponse);

        // Parse the bytes written to the DataOutputStream
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("IMAGE_RESPONSE", tag);

        // Deserialize the server response
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("corr-6", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.READ_TARGET_AVATAR, payloadResponse.getServerResponseMessage());
        assertArrayEquals(mockAvatarBytes, payloadResponse.getBinaryPayload());
    }

}
