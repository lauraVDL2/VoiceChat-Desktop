package org.shared.calendar;

import com.microsoft.graph.models.Calendar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.server.calendar.CalendarRequester;
import org.server.calendar.GraphClient;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.ServerResponse;

import java.net.Socket;

public class GraphClientTest {

    /*@Test
    public void testGetUserCalendar() {
        var client = GraphClient.getClient(JsonMapper.getJsonMapper(), new Socket(), new Message(), new ServerResponse());

        var calendar = new CalendarRequester().getUserCalendar(client);

        //Assertions.assertNotNull(calendar);
    }*/
}
