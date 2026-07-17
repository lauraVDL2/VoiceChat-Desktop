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
import org.shared.pojo.VoiceChatCalendar;
import org.shared.pojo.VoiceChatEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class CalendarAction extends AbstractAction {

    public void getEvents(ObjectMapper objectMapper, Message messageObj,
                          ServerResponse serverResponse, Socket socket,
                          GraphServiceClient  graphServiceClient, VoiceChatCalendar calendar) throws IOException {
        if (calendar != null) {
            CalendarRequester calendarRequester = new CalendarRequester();
            var page = calendarRequester.getCalendarView(graphServiceClient, calendar.getStartWeekTime(), calendar.getEndWeekTime());
            List<VoiceChatEvent> events = EventMapper.eventListMap(page.getCurrentPage());
            byte[] bytes = buildSuccessResponseWithPayload(serverResponse, ServerResponseMessage.EVENTS_GET, messageObj.getCorrelationId(),
                    events, objectMapper);
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
            byte[] bytes = buildSuccessResponseWithPayload(serverResponse, ServerResponseMessage.EVENT_CREATED, messageObj.getCorrelationId(),
                    voiceChatEvent, objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    public void deleteEvent(ObjectMapper objectMapper, Message messageObj,
                            ServerResponse serverResponse, Socket socket,
                            GraphServiceClient  graphServiceClient, VoiceChatEvent event) throws IOException {
        if (event != null) {
            CalendarRequester calendarRequester = new CalendarRequester();
            boolean isDeleted = calendarRequester.deleteEventInCalendar(graphServiceClient, event);
            if (isDeleted) {
                byte[] bytes = buildSuccessResponse(serverResponse, ServerResponseMessage.EVENT_DELETED, messageObj.getCorrelationId(),
                        objectMapper);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                byte[] bytes = buildFailureResponse(serverResponse, ServerResponseMessage.EVENT_DELETED, messageObj.getCorrelationId(),
                        "Could not delete this event !", objectMapper);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
    }
}
