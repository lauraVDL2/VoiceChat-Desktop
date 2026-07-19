package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.dao.SettingsDao;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsActionTest {
    private Settings settings;
    private User user;

    @BeforeEach
    public void setup() {
        settings = new Settings();
        settings.setThemeMode(ThemeMode.DARK);
        user = new User();
        user.setId(1L);
        user.setEmailAddress("toto.test@yahoo.fr");
        user.setSettings(settings);
    }

    @Test
    void setThemeMode_success_writesJsonResponse() throws Exception {
        SettingsDao settingsDao = mock(SettingsDao.class);
        SettingsAction settingsAction = new SettingsAction(settingsDao);

        ObjectMapper jsonMapper = new ObjectMapper();
        ServerResponse serverResponse = new ServerResponse();

        when(settingsDao.setThemeMode(any())).thenReturn(settings);

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.THEME_MODE_SET,
                jsonMapper.writeValueAsString(user));
        messageObj.setCorrelationId("test-correlation-id");

        settingsAction.setThemeMode(jsonMapper, messageObj, serverResponse, socket);

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
        org.shared.entity.Settings result = jsonMapper.readValue(inner, Settings.class);
        assertEquals(result.getThemeMode(), settings.getThemeMode());
    }

}
