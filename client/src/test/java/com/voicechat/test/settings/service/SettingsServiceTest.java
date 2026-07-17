package com.voicechat.test.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.common.UserSession;
import com.voicechat.client.settings.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.shared.JsonMapper;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SettingsServiceTest {
    private SettingsService settingsService;
    private ServerReader serverReader;
    private PrintWriter printWriter;
    private String correlationId = "test-correlation-id";
    private MockedStatic<Listener> listenerMock;
    private User user;

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

        user = new User();
        user.setDisplayName("test");
        user.setEmailAddress("test.test@example.com");
        user.setUserName("test");
        Settings settings = new Settings();
        settings.setThemeMode(ThemeMode.DARK);
        user.setSettings(settings);
        UserSession.INSTANCE.setUser(user);

        settingsService = new SettingsService();
    }

    @Test
    void testSetThemeMode_writesCorrectJsonResponse() throws Exception {
        settingsService.setThemeMode(user);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.THEME_MODE_SET, message.getMessageType());

        User result = mapper.readValue(message.getPayload(), User.class);

        assertNotNull(result.getSettings());
        assertEquals(result.getSettings().getThemeMode(), user.getSettings().getThemeMode());
    }
}
