package com.voicechat.test.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.ServerReader;
import com.voicechat.client.calendar.service.CalendarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.shared.JsonMapper;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.pojo.VoiceChatCalendar;
import org.shared.pojo.VoiceChatEvent;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CalendarServiceTest {
    private CalendarService calendarService;
    private ServerReader serverReader;
    private PrintWriter printWriter;
    private String correlationId = "test-correlation-id";
    private MockedStatic<Listener> listenerMock;
    private VoiceChatCalendar voiceChatCalendar;
    private VoiceChatEvent voiceChatEvent;

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

        voiceChatCalendar = new VoiceChatCalendar();
        voiceChatCalendar.setOwnerEmailAddress("toto.toto@example.com");
        voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setOrganizer("toto.toto@example.com");

        calendarService = new CalendarService();
    }

    @Test
    void testGetEvents_writesCorrectJsonResponse() throws Exception {
        calendarService.getEvents(voiceChatCalendar);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.EVENTS_GET, message.getMessageType());

        VoiceChatCalendar readCalendar = mapper.readValue(message.getPayload(), VoiceChatCalendar.class);

        assertEquals(readCalendar.getOwnerEmailAddress(), voiceChatCalendar.getOwnerEmailAddress());
    }

    @Test
    void testCreateEvent_writesCorrectJsonResponse() throws Exception {
        calendarService.createEvent(voiceChatEvent);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.EVENT_CREATE, message.getMessageType());

        VoiceChatEvent readEvent = mapper.readValue(message.getPayload(), VoiceChatEvent.class);

        assertEquals(readEvent.getOrganizer(), voiceChatEvent.getOrganizer());
    }

    @Test
    void testDeleteEvent_writesCorrectJsonResponse() throws Exception {
        calendarService.deleteEvent(voiceChatEvent);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).println(captor.capture());
        verify(printWriter).flush();

        String sentJson = captor.getValue();

        // Deserialize the JSON string back to Message object
        ObjectMapper mapper = JsonMapper.getJsonMapper();
        org.shared.Message message = mapper.readValue(sentJson, org.shared.Message.class);

        assertSame(MessageType.EVENT_DELETE, message.getMessageType());

        VoiceChatEvent readEvent = mapper.readValue(message.getPayload(), VoiceChatEvent.class);

        assertEquals(readEvent.getOrganizer(), voiceChatEvent.getOrganizer());
    }

    @AfterEach
    public void tearDown() {
        listenerMock.close();
    }

}
