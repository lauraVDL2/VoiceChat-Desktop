package org.server.microsoft_graph.mapper;

import com.microsoft.graph.models.*;
import org.server.Server;
import org.server.microsoft_graph.pojo.MicrosoftUser;
import org.shared.pojo.VoiceChatEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EventMapper {

    public static List<VoiceChatEvent> eventListMap(List<Event> events) {
        List<VoiceChatEvent> voiceChatEvents = new ArrayList<>();
        for (Event event : events) {
            VoiceChatEvent voiceChatEvent = eventMap(event);
            voiceChatEvents.add(voiceChatEvent);
        }
        return voiceChatEvents;
    }

    public static Event createEventMap(VoiceChatEvent voiceChatEvent) {
        Event event = new Event();
        event.subject = voiceChatEvent.getSubject();
        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = voiceChatEvent.getStart();
        start.timeZone = voiceChatEvent.getTimezone();
        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = voiceChatEvent.getEnd();
        end.timeZone = voiceChatEvent.getTimezone();
        /*LinkedList<Attendee> attendees = new LinkedList<>();
        Attendee attendee = new Attendee();
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.address = voiceChatEvent.getOrganizer();
        attendee.emailAddress = emailAddress;
        attendees.add(attendee);
        event.attendees = attendees;*/
        event.start = start;
        event.end = end;
        event.isOnlineMeeting = voiceChatEvent.getOnlineMeeting();

        return event;
    }

    public static VoiceChatEvent eventMap(Event event) {
        VoiceChatEvent voiceChatEvent = new VoiceChatEvent();
        voiceChatEvent.setId(event.id);
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
        return voiceChatEvent;
    }
}
