package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.requests.GraphServiceClient;
import org.server.Server;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.pojo.Camera;
import org.shared.pojo.ScreenShare;
import org.shared.pojo.Voice;
import org.shared.pojo.VoiceChatEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

public class MeetingAction extends AbstractAction {

    public void captureScreen(ObjectMapper objectMapper, Message messageObj,
                              ServerResponse serverResponse) throws IOException {
        ScreenShare screenShare = objectMapper.readValue(messageObj.getBinaryPayload(), ScreenShare.class);
        Set<String> participants = Server.meetingParticipants.get(screenShare.getMeetingId());
        for (String participant : participants) {
            Socket targetSocket = Server.userSockets.get(participant);
            if (targetSocket == null || targetSocket.isClosed()) continue;
            DataOutputStream dos = new DataOutputStream(targetSocket.getOutputStream());
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(screenShare));
            serverResponse.getUserMessageMap().computeIfAbsent("screen-" + participant, k -> "screen-" + messageObj.getCorrelationId());
            serverResponse.setCorrelationId("screen-" + messageObj.getCorrelationId());
            serverResponse.setServerResponseMessage(ServerResponseMessage.IS_SCREEN_SHARING);
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
            //System.out.println("RESPONSE" + objectMapper.writeValueAsString(serverResponse));
            dos.writeUTF("JSON_RESPONSE");
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }
    }

    public void captureVideo(ObjectMapper objectMapper, Message messageObj,
                             ServerResponse serverResponse) throws IOException {
        Camera camera = objectMapper.readValue(messageObj.getBinaryPayload(), Camera.class);
        Set<String> participants = Server.meetingParticipants.get(camera.getMeetingId());
        for (String participant : participants) {
            Socket targetSocket = Server.userSockets.get(participant);
            if (targetSocket == null || targetSocket.isClosed()) continue;
            DataOutputStream dos = new DataOutputStream(targetSocket.getOutputStream());
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(camera));
            serverResponse.getUserMessageMap().computeIfAbsent("camera-" + participant, k -> "camera-" + messageObj.getCorrelationId());
            serverResponse.setCorrelationId("camera-" + messageObj.getCorrelationId());
            serverResponse.setServerResponseMessage(ServerResponseMessage.HAS_CAMERA);
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
            //System.out.println("RESPONSE" + objectMapper.writeValueAsString(serverResponse));
            dos.writeUTF("JSON_RESPONSE");
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }
    }

    public void captureAudio(ObjectMapper objectMapper, Message messageObj,
                             ServerResponse serverResponse, Socket socket) throws IOException {
        // Deserialize the Voice object
        Voice voice = objectMapper.readValue(messageObj.getBinaryPayload(), Voice.class);
        Set<String> participants = Server.meetingParticipants.get(voice.getMeetingId());

        for (String participant : participants) {
            Socket targetSocket = Server.userSockets.get(participant);
            if (targetSocket == null || targetSocket.isClosed()) continue;
            DataOutputStream dos = new DataOutputStream(targetSocket.getOutputStream());
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(voice));
            serverResponse.getUserMessageMap().computeIfAbsent("voice-" + participant, k -> "voice-" + messageObj.getCorrelationId());
            serverResponse.setCorrelationId("voice-" + messageObj.getCorrelationId());
            serverResponse.setServerResponseMessage(ServerResponseMessage.IS_TALKING);
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
            //System.out.println("RESPONSE" + objectMapper.writeValueAsString(serverResponse));
            dos.writeUTF("JSON_RESPONSE");
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }
    }

    public VoiceChatEvent connect(ObjectMapper objectMapper, Message messageObj,
                          ServerResponse serverResponse, Socket socket) throws IOException {
        VoiceChatEvent event = objectMapper.readValue(messageObj.getPayload(), VoiceChatEvent.class);
        if (event != null) {
            byte[] bytes = buildSuccessResponse(serverResponse, ServerResponseMessage.MEETING_CONNECTED, messageObj.getCorrelationId(),
                    objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            return event;
        }
        return null;
    }
}
