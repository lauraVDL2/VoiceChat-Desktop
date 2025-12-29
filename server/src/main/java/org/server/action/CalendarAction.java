package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.requests.GraphServiceClient;
import org.server.microsoft_graph.mapper.EventMapper;
import org.server.microsoft_graph.requester.CalendarRequester;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.mapped_entity.VoiceChatCalendar;
import org.shared.mapped_entity.VoiceChatEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class CalendarAction {

    public void getEvents(ObjectMapper objectMapper, Message messageObj,
                          ServerResponse serverResponse, Socket socket,
                          GraphServiceClient  graphServiceClient, VoiceChatCalendar calendar) throws IOException {
        if (calendar != null) {
            CalendarRequester calendarRequester = new CalendarRequester();
            var page = calendarRequester.getCalendarView(graphServiceClient, calendar.getStartWeekTime(), calendar.getEndWeekTime());
            List<VoiceChatEvent> events = EventMapper.eventListMap(page.getCurrentPage());
            byte[] bytes = null;
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.EVENTS_GET);
            serverResponse.setCorrelationId(messageObj.getCorrelationId());
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(events));
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    public void createEvent(ObjectMapper objectMapper, Message messageObj,
                            ServerResponse serverResponse, Socket socket,
                            GraphServiceClient  graphServiceClient, VoiceChatEvent event) throws IOException {
        if (event != null) {
            CalendarRequester calendarRequester = new CalendarRequester();
            VoiceChatEvent voiceChatEvent = calendarRequester.createEventInCalendar(graphServiceClient, event);
            byte[] bytes = null;
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.EVENT_CREATED);
            serverResponse.setCorrelationId(messageObj.getCorrelationId());
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(voiceChatEvent));
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
