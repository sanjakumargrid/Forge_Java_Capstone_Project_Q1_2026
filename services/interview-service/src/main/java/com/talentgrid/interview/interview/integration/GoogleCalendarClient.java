package com.talentgrid.interview.interview.integration;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.entity.Interview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarClient {

    private final Calendar googleCalendarService;

    /**
     * REQ-ER-07: Checks if any interviewer has a conflicting calendar event at the
     * scheduled interview time, then creates a Google Calendar event with an
     * auto-generated Google Meet video link.
     *
     * @param interview the interview entity with scheduledAt, durationMins, timeZone, interviewers
     * @return GoogleCalendarResponse containing the real eventId and Google Meet link
     */
    public GoogleCalendarResponse createEvent(Interview interview) {
        try {
            ZoneId zone = ZoneId.of(interview.getTimeZone());

            ZonedDateTime startZdt = interview.getScheduledAt().atZone(zone);
            ZonedDateTime endZdt = startZdt.plusMinutes(interview.getDurationMins());

            // 1. FreeBusy check — verify all interviewers are available at this time
            checkInterviewerAvailability(interview, startZdt, endZdt);

            // 2. Build attendee list
            List<EventAttendee> attendees = buildAttendees(interview);

            // 3. Build the Calendar Event object
            Event event = new Event()
                    .setSummary("TalentGrid Interview — Application #" + interview.getApplicationId())
                    .setDescription(buildEventDescription(interview))
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setAttendees(attendees)
                    // 4. Request Google Meet conference link auto-generation
                    .setConferenceData(new ConferenceData()
                            .setCreateRequest(new CreateConferenceRequest()
                                    .setRequestId(UUID.randomUUID().toString())
                                    .setConferenceSolutionKey(
                                            new ConferenceSolutionKey().setType("hangoutsMeet")
                                    )
                            )
                    )
                    .setReminders(new Event.Reminders()
                            .setUseDefault(false)
                            .setOverrides(List.of(
                                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                                    new EventReminder().setMethod("popup").setMinutes(30)
                            ))
                    );

            // 5. Insert event — conferenceDataVersion=1 triggers Meet link generation
            //    sendUpdates=all sends email invites to all attendees
            Event createdEvent = googleCalendarService.events()
                    .insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            // 6. Extract the auto-generated Google Meet link
            String meetLink = extractMeetLink(createdEvent);
            String eventId = createdEvent.getId();

            log.info("[GoogleCalendarClient] Calendar event created | eventId={} | meetLink={}", eventId, meetLink);

            return new GoogleCalendarResponse(eventId, meetLink);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to create Google Calendar event: {}", ex.getMessage());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Google Calendar event: " + ex.getMessage()
            );
        }
    }

    /**
     * REQ-ER-07: Updates an existing Google Calendar event when interview details change
     * (reschedule, interviewer change, etc.).
     *
     * @param eventId   the existing calendar event ID stored on the Interview record
     * @param interview the interview entity with the updated details
     */
    public void updateEvent(String eventId, Interview interview) {
        try {
            ZoneId zone = ZoneId.of(interview.getTimeZone());
            ZonedDateTime startZdt = interview.getScheduledAt().atZone(zone);
            ZonedDateTime endZdt = startZdt.plusMinutes(interview.getDurationMins());

            // Fetch existing event to preserve conference data and other fields
            Event existingEvent = googleCalendarService.events()
                    .get("primary", eventId)
                    .execute();

            // Update mutable fields
            existingEvent
                    .setSummary("TalentGrid Interview — Application #" + interview.getApplicationId())
                    .setDescription(buildEventDescription(interview))
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setAttendees(buildAttendees(interview));

            // sendUpdates=all sends updated invites to all attendees
            googleCalendarService.events()
                    .update("primary", eventId, existingEvent)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            log.info("[GoogleCalendarClient] Calendar event updated | eventId={}", eventId);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to update Google Calendar event eventId={}: {}",
                    eventId, ex.getMessage());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to update Google Calendar event: " + ex.getMessage()
            );
        }
    }

    /**
     * REQ-ER-07: Deletes a Google Calendar event when an interview is canceled.
     * Sends cancellation emails to all attendees via sendUpdates=all.
     *
     * @param eventId the calendar event ID to delete
     */
    public void deleteEvent(String eventId) {
        try {
            googleCalendarService.events()
                    .delete("primary", eventId)
                    .setSendUpdates("all")
                    .execute();

            log.info("[GoogleCalendarClient] Calendar event deleted | eventId={}", eventId);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to delete Google Calendar event eventId={}: {}",
                    eventId, ex.getMessage());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to delete Google Calendar event: " + ex.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uses the Google Calendar FreeBusy API to check if any of the interviewers
     * already have a conflicting event at the proposed interview time.
     * Throws a BusinessException listing which interviewers are unavailable.
     */
    private void checkInterviewerAvailability(
            Interview interview,
            ZonedDateTime startZdt,
            ZonedDateTime endZdt
    ) throws IOException {

        List<FreeBusyRequestItem> items = new ArrayList<>();
        for (Long interviewerId : interview.getInterviewers()) {
            items.add(new FreeBusyRequestItem()
                    .setId("interviewer-" + interviewerId + "@talentgrid.com"));
        }

        FreeBusyRequest freeBusyRequest = new FreeBusyRequest()
                .setTimeMin(new DateTime(startZdt.toInstant().toEpochMilli()))
                .setTimeMax(new DateTime(endZdt.toInstant().toEpochMilli()))
                .setTimeZone(interview.getTimeZone())
                .setItems(items);

        FreeBusyResponse freeBusyResponse = googleCalendarService.freebusy()
                .query(freeBusyRequest)
                .execute();

        List<String> busyInterviewers = new ArrayList<>();

        for (Long interviewerId : interview.getInterviewers()) {
            String calendarId = "interviewer-" + interviewerId + "@talentgrid.com";
            FreeBusyCalendar calendarBusy = freeBusyResponse.getCalendars().get(calendarId);

            if (calendarBusy != null
                    && calendarBusy.getBusy() != null
                    && !calendarBusy.getBusy().isEmpty()) {
                busyInterviewers.add("Interviewer #" + interviewerId);
            }
        }

        if (!busyInterviewers.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "The following interviewers have a calendar conflict at the scheduled time: "
                            + String.join(", ", busyInterviewers)
                            + ". Please choose a different time slot."
            );
        }

        log.info("[GoogleCalendarClient] All {} interviewers are available for the scheduled slot",
                interview.getInterviewers().size());
    }

    /**
     * Builds the list of Google Calendar attendees from the interview's interviewer IDs.
     * In production, resolve interviewer IDs to real work email addresses from employee-service.
     */
    private List<EventAttendee> buildAttendees(Interview interview) {
        List<EventAttendee> attendees = new ArrayList<>();
        for (Long interviewerId : interview.getInterviewers()) {
            EventAttendee attendee = new EventAttendee();
            attendee.setEmail("interviewer-" + interviewerId + "@talentgrid.com");
            attendee.setResponseStatus("needsAction");
            attendees.add(attendee);
        }
        return attendees;
    }

    /**
     * Extracts the Google Meet video link from the created event's conference data.
     */
    private String extractMeetLink(Event createdEvent) {
        if (createdEvent.getConferenceData() != null
                && createdEvent.getConferenceData().getEntryPoints() != null) {

            return createdEvent.getConferenceData().getEntryPoints()
                    .stream()
                    .filter(ep -> "video".equals(ep.getEntryPointType()))
                    .map(EntryPoint::getUri)
                    .findFirst()
                    .orElse(createdEvent.getHangoutLink());
        }
        return createdEvent.getHangoutLink();
    }

    /**
     * Builds a human-readable event description for the calendar invite body.
     */
    private String buildEventDescription(Interview interview) {
        return String.format(
                """
                TalentGrid Interview Details
                ─────────────────────────────
                Application ID : %d
                Interview Type : %s
                Duration       : %d minutes
                Time Zone      : %s
                
                This invite was automatically generated by TalentGrid.
                Please do not reply to this calendar event directly.
                """,
                interview.getApplicationId(),
                interview.getInterviewType(),
                interview.getDurationMins(),
                interview.getTimeZone()
        );
    }
}