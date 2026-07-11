package org.server.requester;

import com.microsoft.graph.models.Event;
import com.microsoft.graph.requests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.microsoft_graph.mapper.EventMapper;
import org.server.microsoft_graph.requester.CalendarRequester;
import org.shared.pojo.VoiceChatEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CalendarRequesterTest {

    CalendarRequester calendarRequester;
    GraphServiceClient graphServiceClient;

    @BeforeEach
    public void setUp() {
        calendarRequester = new CalendarRequester();
        graphServiceClient = mock(GraphServiceClient.class);
    }

    @Test
    void getCalendarView_success() {
        String startDateTime = "2024-06-01T00:00:00Z";
        String endDateTime = "2024-06-30T23:59:59Z";

        // Mock the chain of calls: me() -> calendar() -> calendarView() -> buildRequest() -> get()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        CalendarRequestBuilder mockCalendarRequestBuilder = mock(CalendarRequestBuilder.class);
        EventCollectionRequestBuilder mockEventCollectionRequestBuilder = mock(EventCollectionRequestBuilder.class);
        EventCollectionRequest mockEventCollectionRequest = mock(EventCollectionRequest.class);
        EventCollectionPage mockEventCollectionPage = mock(EventCollectionPage.class);

        // Stub the chain
        when(graphServiceClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.calendar()).thenReturn(mockCalendarRequestBuilder);
        when(mockCalendarRequestBuilder.calendarView()).thenReturn(mockEventCollectionRequestBuilder);
        when(mockEventCollectionRequestBuilder.buildRequest(any(), any())).thenReturn(mockEventCollectionRequest);
        when(mockEventCollectionRequest.get()).thenReturn(mockEventCollectionPage);

        var result = calendarRequester.getCalendarView(graphServiceClient, startDateTime, endDateTime);

        assertNotNull(result);
    }

    @Test
    void createEventInCalendar_success() {
        // Mock the chain of calls: me() -> calendar() -> events() -> buildRequest() -> post()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        CalendarRequestBuilder mockCalendarRequestBuilder = mock(CalendarRequestBuilder.class);
        EventCollectionRequestBuilder mockEventCollectionRequestBuilder = mock(EventCollectionRequestBuilder.class);
        EventCollectionRequest mockEventCollectionRequest = mock(EventCollectionRequest.class);
        Event mockEvent = new Event();
        mockEvent.id = "event-1";
        VoiceChatEvent voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setId("event-1");

        // Stub the chain
        when(graphServiceClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.calendar()).thenReturn(mockCalendarRequestBuilder);
        when(mockCalendarRequestBuilder.events()).thenReturn(mockEventCollectionRequestBuilder);
        when(mockEventCollectionRequestBuilder.buildRequest()).thenReturn(mockEventCollectionRequest);
        when(mockEventCollectionRequest.post(any(Event.class))).thenReturn(mockEvent);

        var result = calendarRequester.createEventInCalendar(graphServiceClient, voiceChatEvent);

        assertNotNull(result);
        assertEquals("event-1", result.getId());
    }

    @Test
    void deleteEventInCalendar_success() {
        // Mock the chain of calls: me() -> events(eventId) -> buildRequest() -> delete()
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        EventRequestBuilder mockEventRequestBuilder = mock(EventRequestBuilder.class);
        EventRequest mockEventRequest = mock(EventRequest.class);
        VoiceChatEvent voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setId("event-1");

        // Stub the chain
        when(graphServiceClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.events("event-1")).thenReturn(mockEventRequestBuilder);
        when(mockEventRequestBuilder.buildRequest()).thenReturn(mockEventRequest);
        when(mockEventRequest.delete()).thenReturn(EventMapper.createEventMap(voiceChatEvent));

        var result = calendarRequester.deleteEventInCalendar(graphServiceClient, voiceChatEvent);

        assertTrue(result);
    }

}
