package com.talentgrid.interview.interview.integration;

import com.talentgrid.interview.interview.entity.Interview;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class GoogleCalendarClient {

    public GoogleCalendarResponse createEvent(Interview interview) {
        // TODO: Replace with real Google Calendar API integration using OAuth2 + Calendar v3 SDK
        log.warn("[GoogleCalendarClient] createEvent — real Google Calendar API not yet integrated. Returning stub response.");

        String eventId = UUID.randomUUID().toString();
        String meetLink = "https://meet.google.com/stub-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[GoogleCalendarClient] Stub event created | eventId={} | meetLink={}", eventId, meetLink);

        return new GoogleCalendarResponse(eventId, meetLink);
    }

    public void updateEvent(String eventId, Interview interview) {
        // TODO: Replace with real Google Calendar API update call
        log.warn("[GoogleCalendarClient] updateEvent — real Google Calendar API not yet integrated. eventId={}", eventId);
    }

    public void deleteEvent(String eventId) {
        // TODO: Replace with real Google Calendar API delete call
        log.warn("[GoogleCalendarClient] deleteEvent — real Google Calendar API not yet integrated. eventId={}", eventId);
    }
}