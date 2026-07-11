package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.action.MicrosoftUserAction;
import org.shared.*;
import org.shared.pojo.MicrosoftAccount;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MicrosoftUserActionTest {

    User microsoftUser;

    @BeforeEach
    void setUp() {
        microsoftUser = new User();
        microsoftUser.displayName = "toto";
        microsoftUser.mail = "toto.toto@hotmail.com";
    }

    @Test
    void getMicrosoftUser_success_writesJsonResponse() throws Exception {
        MicrosoftUserAction action = new MicrosoftUserAction();
        ObjectMapper jsonMapper = JsonMapper.getJsonMapper();

        ServerResponse serverResponse = new ServerResponse();

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        Message messageObj = new Message(MessageType.MICROSOFT_AUTHENTICATE, "");
        messageObj.setCorrelationId("corr-9");

        // Act
        action.getMicrosoftUser(jsonMapper, messageObj, serverResponse, socket, microsoftUser);

        // Assert serverResponse
        assertEquals("msa-corr-9", serverResponse.getCorrelationId());
        assertEquals(serverResponse.getServerResponseStatus(), ServerResponseStatus.SUCCESS);
        assertEquals(serverResponse.getServerResponseMessage(), ServerResponseMessage.MICROSOFT_AUTHENTICATED);

        // Parse the bytes written to the socket
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);

        // Deserialize the server response written to the socket
        ServerResponse payloadResponse = jsonMapper.readValue(payload, ServerResponse.class);
        assertEquals("msa-corr-9", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.MICROSOFT_AUTHENTICATED, payloadResponse.getServerResponseMessage());

        // Assert binary payload contains the MicrosoftAccount
        assertNotNull(payloadResponse.getBinaryPayload());
        MicrosoftAccount account = jsonMapper.readValue(payloadResponse.getBinaryPayload(), MicrosoftAccount.class);
        assertEquals("toto", account.getDisplayName());
        assertEquals("toto.toto@hotmail.com", account.getEmailAddress());
    }
}
