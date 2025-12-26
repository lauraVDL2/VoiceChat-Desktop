package org.server.calendar;

import com.microsoft.graph.models.Calendar;
import com.microsoft.graph.requests.GraphServiceClient;

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
}
