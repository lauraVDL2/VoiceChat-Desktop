package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.Server;
import org.shared.*;
import org.shared.pojo.Camera;
import org.shared.pojo.ScreenShare;
import org.shared.pojo.Voice;
import org.shared.pojo.VoiceChatEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class MeetingActionTest {

    ConcurrentHashMap<String, Set<String>> meetingParticipants = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Socket> userSockets = new ConcurrentHashMap<>();
    Set<String> participants;
    Socket socket1, socket2;

    @BeforeEach
    void setUp() throws Exception {
        // Create participant list
        String participant1 = "user1";
        String participant2 = "user2";
        participants = new HashSet<>();
        participants.add(participant1);
        participants.add(participant2);

        socket1 = mock(Socket.class);
        socket2 = mock(Socket.class);

        Field field = Server.class.getDeclaredField("meetingParticipants");
        field.setAccessible(true);
        meetingParticipants = (ConcurrentHashMap<String, Set<String>>) field.get(null);
        meetingParticipants.clear();
        meetingParticipants.put("meeting-123", participants);

        Field userSocketField = Server.class.getDeclaredField("userSockets");
        userSocketField.setAccessible(true);
        userSockets = (ConcurrentHashMap<String, Socket>) userSocketField.get(null);
        userSockets.clear();
        userSockets.put(participant1, socket1);
        userSockets.put(participant2, socket2);
    }

    @Test
    void captureScreen_success_broadcastsToParticipants() throws Exception {
        MeetingAction action = new MeetingAction();
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create screen share object
        ScreenShare screenShare = new ScreenShare();
        screenShare.setMeetingId("meeting-123");

        Message messageObj = new Message(MessageType.IS_SCREEN_SHARING, "");
        messageObj.setBinaryPayload(jsonMapper.writeValueAsBytes(screenShare));
        messageObj.setCorrelationId("corr-10");



        // Create mocks for sockets BEFORE mockStatic
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();

        when(socket1.getOutputStream()).thenReturn(baos1);
        when(socket1.isClosed()).thenReturn(false);

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        when(socket2.getOutputStream()).thenReturn(baos2);
        when(socket2.isClosed()).thenReturn(false);

        // Act
        action.captureScreen(jsonMapper, messageObj, serverResponse);

        // Assert first participant received the message
        byte[] written1 = baos1.toByteArray();
        DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(written1));
        String tag1 = dis1.readUTF();
        int len1 = dis1.readInt();
        byte[] payload1 = new byte[len1];
        dis1.readFully(payload1);

        assertEquals("JSON_RESPONSE", tag1);
        ServerResponse response1 = jsonMapper.readValue(payload1, ServerResponse.class);
        assertEquals("screen-corr-10", response1.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, response1.getServerResponseStatus());
        assertEquals(ServerResponseMessage.IS_SCREEN_SHARING, response1.getServerResponseMessage());
        assertNotNull(response1.getBinaryPayload());

        // Assert second participant received the message
        byte[] written2 = baos2.toByteArray();
        DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(written2));
        String tag2 = dis2.readUTF();
        int len2 = dis2.readInt();
        byte[] payload2 = new byte[len2];
        dis2.readFully(payload2);

        assertEquals("JSON_RESPONSE", tag2);
        ServerResponse response2 = jsonMapper.readValue(payload2, ServerResponse.class);
        assertEquals("screen-corr-10", response2.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, response2.getServerResponseStatus());
        assertEquals(ServerResponseMessage.IS_SCREEN_SHARING, response2.getServerResponseMessage());

        // Verify screen share data is in binary payload
        ScreenShare receivedScreenShare = jsonMapper.readValue(response2.getBinaryPayload(), ScreenShare.class);
        assertEquals("meeting-123", receivedScreenShare.getMeetingId());
    }

    @Test
    void captureVideo_success_broadcastsToParticipants() throws Exception {
        MeetingAction action = new MeetingAction();
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create camera object
        Camera camera = new Camera();
        camera.setMeetingId("meeting-123");

        Message messageObj = new Message(MessageType.HAS_CAMERA, "");
        messageObj.setBinaryPayload(jsonMapper.writeValueAsBytes(camera));
        messageObj.setCorrelationId("corr-11");

        // Create mocks for sockets
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        when(socket1.getOutputStream()).thenReturn(baos1);
        when(socket1.isClosed()).thenReturn(false);

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        when(socket2.getOutputStream()).thenReturn(baos2);
        when(socket2.isClosed()).thenReturn(false);

        // Act
        action.captureVideo(jsonMapper, messageObj, serverResponse);

        // Assert first participant received the message
        byte[] written1 = baos1.toByteArray();
        DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(written1));
        String tag1 = dis1.readUTF();
        int len1 = dis1.readInt();
        byte[] payload1 = new byte[len1];
        dis1.readFully(payload1);

        assertEquals("JSON_RESPONSE", tag1);
        ServerResponse response1 = jsonMapper.readValue(payload1, ServerResponse.class);
        assertEquals("camera-corr-11", response1.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, response1.getServerResponseStatus());
        assertEquals(ServerResponseMessage.HAS_CAMERA, response1.getServerResponseMessage());
    }

    @Test
    void captureAudio_success_broadcastsToParticipants() throws Exception {
        MeetingAction action = new MeetingAction();
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create voice object
        Voice voice = new Voice();
        voice.setMeetingId("meeting-123");

        Message messageObj = new Message(MessageType.IS_TALKING, "");
        messageObj.setBinaryPayload(jsonMapper.writeValueAsBytes(voice));
        messageObj.setCorrelationId("corr-12");

        // Create mocks for sockets
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        when(socket1.getOutputStream()).thenReturn(baos1);
        when(socket1.isClosed()).thenReturn(false);

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        when(socket2.getOutputStream()).thenReturn(baos2);
        when(socket2.isClosed()).thenReturn(false);

        // Act
        action.captureAudio(jsonMapper, messageObj, serverResponse, socket1);

        // Assert first participant received the message
        byte[] written1 = baos1.toByteArray();
        DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(written1));
        String tag1 = dis1.readUTF();
        int len1 = dis1.readInt();
        byte[] payload1 = new byte[len1];
        dis1.readFully(payload1);

        assertEquals("JSON_RESPONSE", tag1);
        ServerResponse response1 = jsonMapper.readValue(payload1, ServerResponse.class);
        assertEquals("voice-corr-12", response1.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, response1.getServerResponseStatus());
    }

    @Test
    void connect_success() throws Exception {
        MeetingAction action = new MeetingAction();
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Create voice chat event
        VoiceChatEvent event = new VoiceChatEvent();
        event.setOnlineMeeting(true);
        event.setId("meeting-123");

        Message messageObj = new Message(MessageType.MEETING_CONNECT, jsonMapper.writeValueAsString(event));
        messageObj.setCorrelationId("voice-corr-13");

        // Create mocks for sockets
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        when(socket1.getOutputStream()).thenReturn(baos1);
        when(socket1.isClosed()).thenReturn(false);

        // Act
        action.connect(jsonMapper, messageObj, serverResponse, socket1);

        // Assert participant received the message
        byte[] written1 = baos1.toByteArray();
        DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(written1));
        String tag1 = dis1.readUTF();
        int len1 = dis1.readInt();
        byte[] payload1 = new byte[len1];
        dis1.readFully(payload1);

        assertEquals("JSON_RESPONSE", tag1);
        ServerResponse response1 = jsonMapper.readValue(payload1, ServerResponse.class);
        assertEquals("voice-corr-13", response1.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, response1.getServerResponseStatus());
    }

}
