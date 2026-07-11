package org.server.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.IRequestBuilder;
import com.microsoft.graph.requests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.microsoft_graph.mapper.EventMapper;
import org.server.microsoft_graph.requester.CalendarRequester;
import org.shared.*;
import org.shared.pojo.MicrosoftAccount;
import org.shared.pojo.VoiceChatCalendar;
import org.shared.pojo.VoiceChatEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CalendarActionTest {

    List<VoiceChatEvent> events = new ArrayList<>();
    VoiceChatCalendar calendar;

    @BeforeEach
    void setUp() {
        calendar = new VoiceChatCalendar();
        calendar.setStartWeekTime("2024-06-01T00:00:00Z");
        calendar.setEndWeekTime("2024-06-07T23:59:59Z");

        VoiceChatEvent event = new VoiceChatEvent();
        event.setId("event-123");
        event.setSubject("Test Event");
        events.add(event);
    }

    @Test
    void getEvents_success() throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        CalendarAction action = new CalendarAction();

        ServerResponse serverResponse = new ServerResponse();

        Message messageObj = new Message(MessageType.CALENDAR_GET, "");
        messageObj.setCorrelationId("msa-corr-123");

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        // Mock the builder
        var mockBuilder = mock(GraphServiceClient.Builder.class);

        // Mock the client
        var mockGraphClient = mock(GraphServiceClient.class);

        // Mock the builder's buildClient() method to return your mock client
        when(mockBuilder.buildClient()).thenReturn(mockGraphClient);

        // Mock the chain of calls: me() -> calendar() -> calendarView() -> buildRequest() -> get()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        CalendarRequestBuilder mockCalendarRequestBuilder = mock(CalendarRequestBuilder.class);
        EventCollectionRequestBuilder mockEventCollectionRequestBuilder = mock(EventCollectionRequestBuilder.class);
        EventCollectionRequest mockEventCollectionRequest = mock(EventCollectionRequest.class);
        EventCollectionPage mockEventCollectionPage = mock(EventCollectionPage.class);

        // Stub the chain
        when(mockGraphClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.calendar()).thenReturn(mockCalendarRequestBuilder);
        when(mockCalendarRequestBuilder.calendarView()).thenReturn(mockEventCollectionRequestBuilder);
        when(mockEventCollectionRequestBuilder.buildRequest(any(), any())).thenReturn(mockEventCollectionRequest);
        when(mockEventCollectionRequest.get()).thenReturn(mockEventCollectionPage);

        // Prepare mock events to return
        com.microsoft.graph.models.Event mockEvent = mock(com.microsoft.graph.models.Event.class);
        when(mockEventCollectionPage.getCurrentPage()).thenReturn(List.of(mockEvent));


        action.getEvents(objectMapper, messageObj, serverResponse, socket, mockGraphClient, calendar);

        // Assert serverResponse
        assertEquals("msa-corr-123", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.EVENTS_GET, serverResponse.getServerResponseMessage());

        // Parse the bytes written to the socket
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);

        // Deserialize the server response written to the socket
        ServerResponse payloadResponse = objectMapper.readValue(payload, ServerResponse.class);
        assertEquals("msa-corr-123", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.EVENTS_GET, payloadResponse.getServerResponseMessage());

        // Assert binary payload contains the MicrosoftAccount
        assertNotNull(payloadResponse.getBinaryPayload());
        List<VoiceChatEvent> myEvents = objectMapper.readValue(
                payloadResponse.getBinaryPayload(),
                new TypeReference<List<VoiceChatEvent>>() {}
        );
        assertEquals(1, myEvents.size());
    }

    @Test
    void createEvents_success() throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        CalendarAction action = new CalendarAction();

        ServerResponse serverResponse = new ServerResponse();

        Message messageObj = new Message(MessageType.EVENT_CREATE, "");
        messageObj.setCorrelationId("msa-corr-123");

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        // Mock the builder
        var mockBuilder = mock(GraphServiceClient.Builder.class);

        // Mock the client
        var mockGraphClient = mock(GraphServiceClient.class);

        // Mock the builder's buildClient() method to return your mock client
        when(mockBuilder.buildClient()).thenReturn(mockGraphClient);

        // Mock the chain of calls: me() -> calendar() -> events() -> buildRequest() -> post()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        CalendarRequestBuilder mockCalendarRequestBuilder = mock(CalendarRequestBuilder.class);
        EventCollectionRequestBuilder mockEventCollectionRequestBuilder = mock(EventCollectionRequestBuilder.class);
        EventCollectionRequest mockEventCollectionRequest = mock(EventCollectionRequest.class);
        com.microsoft.graph.models.Event mockEvent = mock(com.microsoft.graph.models.Event.class);

        // Stub the chain
        when(mockGraphClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.calendar()).thenReturn(mockCalendarRequestBuilder);
        when(mockCalendarRequestBuilder.events()).thenReturn(mockEventCollectionRequestBuilder);
        when(mockEventCollectionRequestBuilder.buildRequest()).thenReturn(mockEventCollectionRequest);
        when(mockEventCollectionRequest.post(any())).thenReturn(mockEvent);

        action.createEvent(objectMapper, messageObj, serverResponse, socket, mockGraphClient, events.getFirst());

        // Assert serverResponse
        assertEquals("msa-corr-123", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.EVENT_CREATED, serverResponse.getServerResponseMessage());

        // Parse the bytes written to the socket
        byte[] written = baos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(written));
        String tag = dis.readUTF();
        int len = dis.readInt();
        byte[] payload = new byte[len];
        dis.readFully(payload);

        assertEquals("JSON_RESPONSE", tag);

        // Deserialize the server response written to the socket
        ServerResponse payloadResponse = objectMapper.readValue(payload, ServerResponse.class);
        assertEquals("msa-corr-123", payloadResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, payloadResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.EVENT_CREATED, payloadResponse.getServerResponseMessage());

        // Assert binary payload contains the MicrosoftAccount
        assertNotNull(payloadResponse.getBinaryPayload());
        VoiceChatEvent myEvent = objectMapper.readValue(payloadResponse.getBinaryPayload(), VoiceChatEvent.class);
        assertNotNull(myEvent);
    }

    @Test
    void deleteEvent_success() throws Exception {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        CalendarAction action = new CalendarAction();

        ServerResponse serverResponse = new ServerResponse();

        Message messageObj = new Message(MessageType.EVENT_DELETE, "");
        messageObj.setCorrelationId("msa-corr-123");

        // Capture socket output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(baos);

        // Mock the builder
        var mockBuilder = mock(GraphServiceClient.Builder.class);

        // Mock the client
        var mockGraphClient = mock(GraphServiceClient.class);

        // Mock the builder's buildClient() method to return your mock client
        when(mockBuilder.buildClient()).thenReturn(mockGraphClient);

        // Mock the chain of calls: me() -> events(eventId) -> buildRequest() -> delete()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        EventRequestBuilder mockEventRequestBuilder = mock(EventRequestBuilder.class);
        EventRequest mockEventRequest = mock(EventRequest.class);

        // Stub the chain
        when(mockGraphClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.events(anyString())).thenReturn(mockEventRequestBuilder);
        when(mockEventRequestBuilder.buildRequest()).thenReturn(mockEventRequest);
        when(mockEventRequest.delete()).thenReturn(EventMapper.createEventMap(events.getFirst())); // delete() returns void

        action.deleteEvent(objectMapper, messageObj, serverResponse, socket, mockGraphClient, events.getFirst());

        // Assert serverResponse
        assertEquals("msa-corr-123", serverResponse.getCorrelationId());
        assertEquals(ServerResponseStatus.SUCCESS, serverResponse.getServerResponseStatus());
        assertEquals(ServerResponseMessage.EVENT_DELETED, serverResponse.getServerResponseMessage());
    }

}
