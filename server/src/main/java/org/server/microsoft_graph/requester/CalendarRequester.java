package org.server.microsoft_graph.requester;

import com.microsoft.graph.models.Calendar;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.EventCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import org.server.microsoft_graph.mapper.EventMapper;
import org.shared.mapped_entity.VoiceChatEvent;

public class CalendarRequester {

    public Calendar getUserCalendar(GraphServiceClient graphServiceClient) {
        try {
            Calendar calendar = graphServiceClient
                    .me()
                    .calendar()
                    .buildRequest()
                    .get();
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public EventCollectionPage getCalendarView(GraphServiceClient graphServiceClient, String startDateTime, String endDateTime) {
        try {
            QueryOption start = new QueryOption("startDateTime", startDateTime);
            QueryOption end = new QueryOption("endDateTime", endDateTime);
            var result = graphServiceClient
                    .me()
                    .calendar()
                    .calendarView()
                    .buildRequest(start, end)
                    .get();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public VoiceChatEvent createEventInCalendar(GraphServiceClient graphServiceClient, VoiceChatEvent voiceChatEvent) {
        Event event = EventMapper.createEventMap(voiceChatEvent);
        var result = graphServiceClient
                .me()
                .calendar()
                .events()
                .buildRequest()
                .post(event);
        return EventMapper.eventCreatedMap(result);
    }
}
