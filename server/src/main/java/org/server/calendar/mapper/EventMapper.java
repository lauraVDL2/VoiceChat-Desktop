package org.server.calendar.mapper;

import com.microsoft.graph.models.Event;
import org.shared.mapped_entity.VoiceChatEvent;

import java.util.ArrayList;
import java.util.List;

public class EventMapper {

    public static List<VoiceChatEvent> eventListMap(List<Event> events) {
        List<VoiceChatEvent> voiceChatEvents = new ArrayList<>();
        for (Event event : events) {
            VoiceChatEvent voiceChatEvent = new VoiceChatEvent();
            if (event.end != null) {
                voiceChatEvent.setEnd(event.end.dateTime);
            }
            if (event.start != null) {
                voiceChatEvent.setStart(event.start.dateTime);
            }
            if (event.organizer != null) {
                if (event.organizer.emailAddress != null) {
                    voiceChatEvent.setOrganizer(event.organizer.emailAddress.address);
                }
            }
            voiceChatEvent.setSubject(event.subject);
            voiceChatEvent.setOnlineMeeting(event.isOnlineMeeting);
            voiceChatEvents.add(voiceChatEvent);
        }
        return voiceChatEvents;
    }
}
