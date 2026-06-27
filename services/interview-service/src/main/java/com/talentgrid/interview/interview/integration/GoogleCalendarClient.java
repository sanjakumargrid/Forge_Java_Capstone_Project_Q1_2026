package com.talentgrid.interview.interview.integration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.talentgrid.interview.config.GoogleOAuthTokenService;
import com.talentgrid.interview.interview.entity.Interview;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient {

    private final GoogleOAuthTokenService googleOAuthTokenService;

    @Value("${google.calendar.calendar-id:primary}")
    private String calendarId;

    @Value("${google.calendar.application-name:TalentGrid}")
    private String applicationName;

    public GoogleCalendarResponse createEvent(Interview interview) {

        try {
            Calendar googleCalendar = buildGoogleCalendarService();

            if (interview == null) {
                throw new IllegalArgumentException("Interview is required");
            }

            if (interview.getScheduledAt() == null) {
                throw new IllegalArgumentException("Interview scheduledAt is required");
            }

            String timeZone =
                    interview.getTimeZone() != null && !interview.getTimeZone().isBlank()
                            ? interview.getTimeZone()
                            : "Asia/Kolkata";

            Integer durationMins =
                    interview.getDurationMins() != null
                            ? interview.getDurationMins()
                            : 60;

            LocalDateTime scheduledAt = interview.getScheduledAt();

            ZonedDateTime start =
                    scheduledAt.atZone(ZoneId.of(timeZone));

            ZonedDateTime end =
                    scheduledAt.plusMinutes(durationMins).atZone(ZoneId.of(timeZone));

            Event event = new Event()
                    .setSummary("TalentGrid Interview")
                    .setDescription("Interview scheduled from TalentGrid")
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
                            .setTimeZone(timeZone))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
                            .setTimeZone(timeZone));

            ConferenceData conferenceData =
                    new ConferenceData()
                            .setCreateRequest(
                                    new CreateConferenceRequest()
                                            .setRequestId(UUID.randomUUID().toString())
                                            .setConferenceSolutionKey(
                                                    new ConferenceSolutionKey()
                                                            .setType("hangoutsMeet")
                                            )
                            );

            event.setConferenceData(conferenceData);

            Event createdEvent =
                    googleCalendar
                            .events()
                            .insert(calendarId, event)
                            .setConferenceDataVersion(1)
                            .setSendUpdates("none")
                            .execute();

            String meetLink = createdEvent.getHangoutLink();

            if ((meetLink == null || meetLink.isBlank())
                    && createdEvent.getConferenceData() != null
                    && createdEvent.getConferenceData().getEntryPoints() != null) {

                meetLink = createdEvent.getConferenceData()
                        .getEntryPoints()
                        .stream()
                        .filter(entryPoint ->
                                "video".equalsIgnoreCase(entryPoint.getEntryPointType()))
                        .map(EntryPoint::getUri)
                        .findFirst()
                        .orElse(null);
            }

            if (meetLink == null || meetLink.isBlank()) {
                throw new RuntimeException(
                        "Google Calendar event created, but Google Meet link was not generated"
                );
            }

            return new GoogleCalendarResponse(
                    createdEvent.getId(),
                    meetLink
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create Google Calendar event with Meet link: "
                            + e.getMessage(),
                    e
            );
        }
    }

    public void updateEvent(String calendarEventId, Interview interview) {
        try {
            if (calendarEventId == null || calendarEventId.isBlank()) {
                return;
            }

            Calendar googleCalendar = buildGoogleCalendarService();

            Event existingEvent =
                    googleCalendar.events()
                            .get(calendarId, calendarEventId)
                            .execute();

            if (interview.getScheduledAt() != null) {
                String timeZone =
                        interview.getTimeZone() != null && !interview.getTimeZone().isBlank()
                                ? interview.getTimeZone()
                                : "Asia/Kolkata";

                Integer durationMins =
                        interview.getDurationMins() != null
                                ? interview.getDurationMins()
                                : 60;

                ZonedDateTime start =
                        interview.getScheduledAt().atZone(ZoneId.of(timeZone));

                ZonedDateTime end =
                        interview.getScheduledAt()
                                .plusMinutes(durationMins)
                                .atZone(ZoneId.of(timeZone));

                existingEvent.setStart(new EventDateTime()
                        .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
                        .setTimeZone(timeZone));

                existingEvent.setEnd(new EventDateTime()
                        .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
                        .setTimeZone(timeZone));
            }

            googleCalendar.events()
                    .update(calendarId, calendarEventId, existingEvent)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("none")
                    .execute();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update Google Calendar event: " + e.getMessage(),
                    e
            );
        }
    }

    public void deleteEvent(String calendarEventId) {
        try {
            if (calendarEventId == null || calendarEventId.isBlank()) {
                return;
            }

            Calendar googleCalendar = buildGoogleCalendarService();

            googleCalendar.events()
                    .delete(calendarId, calendarEventId)
                    .execute();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to delete Google Calendar event: " + e.getMessage(),
                    e
            );
        }
    }

    private Calendar buildGoogleCalendarService() throws Exception {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                googleOAuthTokenService.getCredential()
        )
                .setApplicationName(applicationName)
                .build();
    }
}