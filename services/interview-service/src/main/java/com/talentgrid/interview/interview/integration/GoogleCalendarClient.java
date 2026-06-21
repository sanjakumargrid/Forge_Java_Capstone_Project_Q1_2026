package com.talentgrid.interview.interview.integration;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.talentgrid.interview.client.EmployeeClient;
import com.talentgrid.interview.client.dto.EmployeeDto;
import com.talentgrid.interview.config.GoogleOAuthTokenService;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.entity.Interview;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
public class GoogleCalendarClient {

    private final ObjectProvider<Calendar> serviceAccountCalendarProvider;
    private final ObjectProvider<GoogleOAuthTokenService> googleOAuthTokenServiceProvider;
    private final EmployeeClient employeeClient;

    @Value("${google.calendar.enabled:false}")
    private boolean googleCalendarEnabled;

    @Value("${google.calendar.auth-mode:service-account}")
    private String googleCalendarAuthMode;

    @Value("${google.calendar.invites-enabled:false}")
    private boolean calendarInvitesEnabled;

    @Value("${google.calendar.meet-enabled:false}")
    private boolean googleMeetEnabled;

    public GoogleCalendarClient(
            ObjectProvider<Calendar> serviceAccountCalendarProvider,
            ObjectProvider<GoogleOAuthTokenService> googleOAuthTokenServiceProvider,
            EmployeeClient employeeClient
    ) {
        this.serviceAccountCalendarProvider = serviceAccountCalendarProvider;
        this.googleOAuthTokenServiceProvider = googleOAuthTokenServiceProvider;
        this.employeeClient = employeeClient;
    }

    public GoogleCalendarResponse createEvent(Interview interview) {

        Calendar calendarService = getCalendarServiceOrNull();

        if (!googleCalendarEnabled || calendarService == null) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Calendar integration is currently disabled or unavailable. Cannot schedule interview."
            );
        }

        try {
            ZoneId zone = ZoneId.of(interview.getTimeZone());

            ZonedDateTime startZdt = interview.getScheduledAt().atZone(zone);
            ZonedDateTime endZdt = startZdt.plusMinutes(interview.getDurationMins());

            if (calendarInvitesEnabled) {
                checkInterviewerAvailability(calendarService, interview, startZdt, endZdt);
            }

            Event event = new Event()
                    .setSummary("TalentGrid Interview — Application #" + interview.getApplicationId())
                    .setDescription(buildEventDescription(interview))
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setReminders(new Event.Reminders()
                            .setUseDefault(false)
                            .setOverrides(List.of(
                                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                                    new EventReminder().setMethod("popup").setMinutes(30)
                            ))
                    );

            if (calendarInvitesEnabled) {
                event.setAttendees(buildAttendees(interview));
            }

            if (googleMeetEnabled) {
                event.setConferenceData(new ConferenceData()
                        .setCreateRequest(new CreateConferenceRequest()
                                .setRequestId(UUID.randomUUID().toString())
                                .setConferenceSolutionKey(
                                        new ConferenceSolutionKey().setType("hangoutsMeet")
                                )
                        )
                );
            }

            Calendar.Events.Insert insertRequest = calendarService.events()
                    .insert("primary", event)
                    .setSendUpdates(calendarInvitesEnabled ? "all" : "none");

            if (googleMeetEnabled) {
                insertRequest.setConferenceDataVersion(1);
            }

            Event createdEvent = insertRequest.execute();

            String eventId = createdEvent.getId();
            String meetLink;

            if (googleMeetEnabled) {
                meetLink = extractMeetLink(createdEvent);
            } else {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Google Meet generation is not enabled for this interview."
                );
            }

            log.info("[GoogleCalendarClient] Calendar event created | eventId={} | meetLink={}",
                    eventId,
                    meetLink
            );

            return new GoogleCalendarResponse(eventId, meetLink);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to create Google Calendar event: {}",
                    ex.getMessage()
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Google Calendar event: " + ex.getMessage()
            );
        }
    }

    public void updateEvent(String eventId, Interview interview) {

        Calendar calendarService = getCalendarServiceOrNull();

        if (!googleCalendarEnabled
                || calendarService == null
                || eventId == null) {

            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot update interview: Google Calendar integration is disabled or eventId is missing."
            );
        }

        try {
            ZoneId zone = ZoneId.of(interview.getTimeZone());

            ZonedDateTime startZdt = interview.getScheduledAt().atZone(zone);
            ZonedDateTime endZdt = startZdt.plusMinutes(interview.getDurationMins());

            Event existingEvent = calendarService.events()
                    .get("primary", eventId)
                    .execute();

            existingEvent
                    .setSummary("TalentGrid Interview — Application #" + interview.getApplicationId())
                    .setDescription(buildEventDescription(interview))
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                            .setTimeZone(interview.getTimeZone()));

            if (calendarInvitesEnabled) {
                existingEvent.setAttendees(buildAttendees(interview));
            } else {
                existingEvent.setAttendees(null);
            }

            Calendar.Events.Update updateRequest = calendarService.events()
                    .update("primary", eventId, existingEvent)
                    .setSendUpdates(calendarInvitesEnabled ? "all" : "none");

            if (googleMeetEnabled) {
                updateRequest.setConferenceDataVersion(1);
            }

            updateRequest.execute();

            log.info("[GoogleCalendarClient] Calendar event updated | eventId={}", eventId);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to update Google Calendar event eventId={}: {}",
                    eventId,
                    ex.getMessage()
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to update Google Calendar event: " + ex.getMessage()
            );
        }
    }

    public void deleteEvent(String eventId) {

        Calendar calendarService = getCalendarServiceOrNull();

        if (!googleCalendarEnabled
                || calendarService == null
                || eventId == null) {

            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot delete interview: Google Calendar integration is disabled or eventId is missing."
            );
        }

        try {
            calendarService.events()
                    .delete("primary", eventId)
                    .setSendUpdates(calendarInvitesEnabled ? "all" : "none")
                    .execute();

            log.info("[GoogleCalendarClient] Calendar event deleted | eventId={}", eventId);

        } catch (IOException ex) {
            log.error("[GoogleCalendarClient] Failed to delete Google Calendar event eventId={}: {}",
                    eventId,
                    ex.getMessage()
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to delete Google Calendar event: " + ex.getMessage()
            );
        }
    }

    private Calendar getCalendarServiceOrNull() {

        if (!googleCalendarEnabled) {
            return null;
        }

        if ("oauth".equalsIgnoreCase(googleCalendarAuthMode)) {
            GoogleOAuthTokenService googleOAuthTokenService =
                    googleOAuthTokenServiceProvider.getIfAvailable();

            if (googleOAuthTokenService == null || !googleOAuthTokenService.isAuthorized()) {
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Google OAuth is not authorized. Open http://localhost:8084/api/google/oauth/authorize first."
                );
            }

            return googleOAuthTokenService.getCalendarService();
        }

        return serviceAccountCalendarProvider.getIfAvailable();
    }

    private void checkInterviewerAvailability(
            Calendar calendarService,
            Interview interview,
            ZonedDateTime startZdt,
            ZonedDateTime endZdt
    ) throws IOException {

        List<FreeBusyRequestItem> items = new ArrayList<>();

        for (Long interviewerId : interview.getInterviewers()) {
            EmployeeDto employee = employeeClient.getEmployee(interviewerId);
            items.add(new FreeBusyRequestItem()
                    .setId(employee.getEmail()));
        }

        FreeBusyRequest freeBusyRequest = new FreeBusyRequest()
                .setTimeMin(new DateTime(startZdt.toInstant().toEpochMilli()))
                .setTimeMax(new DateTime(endZdt.toInstant().toEpochMilli()))
                .setTimeZone(interview.getTimeZone())
                .setItems(items);

        FreeBusyResponse freeBusyResponse = calendarService.freebusy()
                .query(freeBusyRequest)
                .execute();

        List<String> busyInterviewers = new ArrayList<>();

        for (Long interviewerId : interview.getInterviewers()) {
            EmployeeDto employee = employeeClient.getEmployee(interviewerId);
            String calendarId = employee.getEmail();
            FreeBusyCalendar calendarBusy = freeBusyResponse.getCalendars().get(calendarId);

            if (calendarBusy != null
                    && calendarBusy.getBusy() != null
                    && !calendarBusy.getBusy().isEmpty()) {

                busyInterviewers.add(employee.getName() + " (" + employee.getEmail() + ")");
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
                interview.getInterviewers().size()
        );
    }

    private List<EventAttendee> buildAttendees(Interview interview) {

        List<EventAttendee> attendees = new ArrayList<>();

        for (Long interviewerId : interview.getInterviewers()) {
            EmployeeDto employee = employeeClient.getEmployee(interviewerId);
            EventAttendee attendee = new EventAttendee();
            attendee.setEmail(employee.getEmail());
            attendee.setDisplayName(employee.getName());
            attendee.setResponseStatus("needsAction");
            attendees.add(attendee);
        }

        return attendees;
    }

    private String extractMeetLink(Event createdEvent) {

        if (createdEvent.getConferenceData() != null
                && createdEvent.getConferenceData().getEntryPoints() != null) {

            return createdEvent.getConferenceData().getEntryPoints()
                    .stream()
                    .filter(entryPoint -> "video".equals(entryPoint.getEntryPointType()))
                    .map(EntryPoint::getUri)
                    .findFirst()
                    .orElse(createdEvent.getHangoutLink());
        }

        return createdEvent.getHangoutLink();
    }

    private String buildEventDescription(Interview interview) {

        return String.format(
                """
                TalentGrid Interview Details
                ─────────────────────────────
                Application ID : %d
                Interview Type : %s
                Duration       : %d minutes
                Time Zone      : %s
                
                This calendar event was automatically generated by TalentGrid.
                """,
                interview.getApplicationId(),
                interview.getInterviewType(),
                interview.getDurationMins(),
                interview.getTimeZone()
        );
    }
}