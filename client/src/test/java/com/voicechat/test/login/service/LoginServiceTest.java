package com.voicechat.test.login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.login.service.LoginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.entity.User;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoginServiceTest {
    private LoginService loginService;
    private PrintWriter printWriter;
    private User user;
    private String correlationId = "test-correlation-id";
    private MockedStatic<Listener> listenerMock;

    @BeforeEach
    public void setup() throws Exception {
        listenerMock = Mockito.mockStatic(Listener.class);
        printWriter = mock(PrintWriter.class);
        listenerMock.when(Listener::getServerOut).thenReturn(printWriter);

        user = new User();
        user.setUserName("test");
        user.setPassword("password123");
        user.setEmailAddress("test.test@example.com");

        loginService = new LoginService();
    }

    @Test
    void testLogin_writesCorrectJsonToServer() throws Exception {
        loginService.login(user, correlationId);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        Message message = mapper.readValue(sentJson, Message.class);

        // Verify message fields
        assertSame(MessageType.USER_LOG_IN, message.getMessageType());

        User readUser = mapper.readValue(message.getPayload(), User.class);

        assertEquals(readUser.getEmailAddress(), user.getEmailAddress());
        assertEquals(message.getCorrelationId(), correlationId);
    }

    @AfterEach
    public void tearDown() {
        listenerMock.close();
    }
}
