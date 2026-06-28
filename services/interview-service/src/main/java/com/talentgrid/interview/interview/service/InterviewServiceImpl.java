package com.talentgrid.interview.interview.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.interview.client.ApplicationClient;
import com.talentgrid.interview.client.CandidateClient;
import com.talentgrid.interview.client.EmployeeClient;
import com.talentgrid.interview.client.dto.CandidateDto;
import com.talentgrid.interview.client.dto.EmployeeDto;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.dto.ApplicationDto;
import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.entity.Interviewer;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import com.talentgrid.interview.interview.integration.GoogleCalendarClient;
import com.talentgrid.interview.interview.integration.GoogleCalendarResponse;
import com.talentgrid.interview.interview.mapper.InterviewMapper;
import com.talentgrid.interview.interview.repository.InterviewRepository;
import com.talentgrid.interview.interview.repository.InterviewerRepository;
import com.talentgrid.interview.kafka.producer.InterviewEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewerRepository interviewerRepository;

    private final ApplicationClient applicationClient;
    private final EmployeeClient employeeClient;
    private final GoogleCalendarClient googleCalendarClient;
    private final InterviewEventProducer interviewEventProducer;
    private final AuditLogClient auditLogClient;
    private final CandidateClient candidateClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    public InterviewDto createInterview(InterviewDto interviewDto) {

        if (interviewDto == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Interview details are required"
            );
        }

        Long applicationId = interviewDto.getApplicationId();

        if (applicationId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Application id is required"
            );
        }

        ApplicationDto applicationDto =
                applicationClient.getApplication(applicationId);

        if (applicationDto == null || applicationDto.getApplicationId() == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Application not found with id: " + applicationId
            );
        }

        if ("TECHNICAL".equalsIgnoreCase(applicationDto.getCurrentStage())) {
            applicationClient.moveApplicationStage(
                    applicationId,
                    "INTERVIEW",
                    "Interview scheduled"
            );
        } else if (!"INTERVIEW".equalsIgnoreCase(applicationDto.getCurrentStage())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Interview can be scheduled only when application is in TECHNICAL or INTERVIEW stage. Current stage is "
                            + applicationDto.getCurrentStage()
            );
        }

        if (applicationDto.getCandidateId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Application does not have an associated candidate"
            );
        }

        CandidateDto candidate =
                candidateClient.getCandidate(applicationDto.getCandidateId());

        if (candidate == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Candidate not found"
            );
        }

        Interview interview =
                InterviewMapper.dtoToEntity(interviewDto);

        if (interview.getStatus() == null) {
            interview.setStatus(Status.SCHEDULED);
        }

        /*
         * Important:
         * This sets interview_id in interviewer table.
         */
        attachInterviewersToInterview(interview);

        /*
         * Save interview first, then create Google Calendar event.
         */
        Interview savedInterview =
                interviewRepository.saveAndFlush(interview);

        /*
         * Real Google Meet link creation.
         * Do not continue silently when Google Calendar fails.
         */
        GoogleCalendarResponse response;

        try {
            response = googleCalendarClient.createEvent(savedInterview);

            if (response == null
                    || response.getEventId() == null
                    || response.getMeetLink() == null
                    || response.getMeetLink().isBlank()) {
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Google Meet link was not generated. Please check Google Calendar OAuth and conferenceDataVersion."
                );
            }

            savedInterview.setCalendarEventId(response.getEventId());
            savedInterview.setMeetLink(response.getMeetLink());

            savedInterview = interviewRepository.save(savedInterview);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "[InterviewService] Google Calendar creation failed",
                    e
            );

            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Calendar creation failed: " + e.getMessage()
            );
        }

        String interviewerName = "Our Team";

        if (savedInterview.getInterviewers() != null
                && !savedInterview.getInterviewers().isEmpty()) {
            try {
                Interviewer firstInterviewer =
                        savedInterview.getInterviewers().get(0);

                EmployeeDto primaryInterviewer =
                        employeeClient.getEmployee(firstInterviewer.getEmployeeId());

                if (primaryInterviewer != null
                        && primaryInterviewer.getName() != null) {
                    interviewerName = primaryInterviewer.getName();
                }
            } catch (Exception e) {
                log.warn(
                        "[InterviewService] Could not resolve primary interviewer name: {}",
                        e.getMessage()
                );
            }
        }

        String candidateName =
                buildCandidateName(candidate);

        String meetLink =
                savedInterview.getMeetLink() != null
                        ? savedInterview.getMeetLink()
                        : "TBD";

        notificationEventPublisher.sendInAppAndEmail(
                applicationDto.getCandidateId().toString(),
                candidate.getEmail(),
                null,
                "INTERVIEW_INVITATION",
                "Interview Invitation from Grid Dynamics",
                "You have been invited to an interview. Please join using the Google Meet link: "
                        + meetLink,
                "interview-service",
                savedInterview.getInterviewId().toString(),
                "INTERVIEW",
                "HIGH",
                "interview-invitation",
                Map.of(
                        "candidateName", candidateName,
                        "companyName", "Grid Dynamics",
                        "interviewDate",
                        savedInterview.getScheduledAt() != null
                                ? savedInterview.getScheduledAt().toString()
                                : "TBD",
                        "meetLink", meetLink,
                        "interviewerName", interviewerName
                ),
                UUID.randomUUID().toString()
        );

        if (savedInterview.getInterviewers() != null) {
            for (Interviewer interviewer : savedInterview.getInterviewers()) {
                try {
                    EmployeeDto employee =
                            employeeClient.getEmployee(interviewer.getEmployeeId());

                    if (employee != null && employee.getEmail() != null) {
                        String employeeName =
                                employee.getName() != null
                                        ? employee.getName()
                                        : "Interviewer";

                        notificationEventPublisher.sendInAppAndEmail(
                                String.valueOf(interviewer.getEmployeeId()),
                                employee.getEmail(),
                                null,
                                "INTERVIEW_INVITATION",
                                "Interview Scheduled: " + candidateName,
                                "You have been scheduled to interview "
                                        + candidateName
                                        + ". Please join using the Google Meet link: "
                                        + meetLink,
                                "interview-service",
                                savedInterview.getInterviewId().toString(),
                                "INTERVIEW",
                                "HIGH",
                                "interviewer-invitation",
                                Map.of(
                                        "candidateName", candidateName,
                                        "companyName", "Grid Dynamics",
                                        "interviewDate",
                                        savedInterview.getScheduledAt() != null
                                                ? savedInterview.getScheduledAt().toString()
                                                : "TBD",
                                        "meetLink", meetLink,
                                        "interviewerName", employeeName
                                ),
                                UUID.randomUUID().toString()
                        );
                    }
                } catch (Exception e) {
                    log.error(
                            "[InterviewService] Failed to send email to interviewer {}: {}",
                            interviewer.getEmployeeId(),
                            e.getMessage()
                    );
                }
            }
        }

        interviewEventProducer.publishScheduled(savedInterview);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("INTERVIEW")
                        .entityId(savedInterview.getInterviewId())
                        .action(AuditAction.CREATE)
                        .afterState(Map.of(
                                "applicationId", savedInterview.getApplicationId(),
                                "status", savedInterview.getStatus().name(),
                                "meetLink", savedInterview.getMeetLink(),
                                "calendarEventId", savedInterview.getCalendarEventId()
                        ))
                        .serviceName("interview-service")
                        .endpoint("/api/v1/interviews")
                        .build()
        );

        return InterviewMapper.entityToDto(savedInterview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewDto getInterviewById(Long id) {

        Interview interview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        return InterviewMapper.entityToDto(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewDto> getAllInterviews(
            Long applicationId,
            Status status,
            Type interviewType,
            Long interviewerId,
            Pageable pageable
    ) {

        Page<Interview> interviews =
                interviewRepository.findWithFilters(
                        applicationId,
                        status,
                        interviewType,
                        interviewerId,
                        pageable
                );

        return interviews.map(InterviewMapper::entityToDto);
    }

    @Override
    @Transactional
    public InterviewDto updateInterview(
            Long id,
            InterviewDto interviewDto
    ) {

        if (interviewDto == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Interview details are required"
            );
        }

        Interview existingInterview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        if (interviewDto.getApplicationId() != null
                && !interviewDto.getApplicationId()
                .equals(existingInterview.getApplicationId())) {

            ApplicationDto applicationDto =
                    applicationClient.getApplication(interviewDto.getApplicationId());

            if (applicationDto == null || applicationDto.getApplicationId() == null) {
                throw new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with id: "
                                + interviewDto.getApplicationId()
                );
            }

            existingInterview.setApplicationId(interviewDto.getApplicationId());
        }

        if (interviewDto.getInterviewers() != null) {
            replaceInterviewers(existingInterview, interviewDto.getInterviewers());
        }

        existingInterview.setInterviewType(interviewDto.getInterviewType());
        existingInterview.setScheduledAt(interviewDto.getScheduledAt());
        existingInterview.setDurationMins(interviewDto.getDurationMins());
        existingInterview.setTimeZone(interviewDto.getTimeZone());

        if (interviewDto.getMeetLink() != null) {
            existingInterview.setMeetLink(interviewDto.getMeetLink());
        }

        if (interviewDto.getCalendarEventId() != null) {
            existingInterview.setCalendarEventId(interviewDto.getCalendarEventId());
        }

        if (interviewDto.getStatus() != null) {
            existingInterview.setStatus(interviewDto.getStatus());
        }

        if (existingInterview.getCalendarEventId() != null) {
            googleCalendarClient.updateEvent(
                    existingInterview.getCalendarEventId(),
                    existingInterview
            );
        }

        Interview savedInterview =
                interviewRepository.save(existingInterview);

        interviewEventProducer.publishUpdated(savedInterview);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("INTERVIEW")
                        .entityId(savedInterview.getInterviewId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", savedInterview.getStatus().name()
                        ))
                        .serviceName("interview-service")
                        .endpoint("/api/v1/interviews/" + id)
                        .build()
        );

        return InterviewMapper.entityToDto(savedInterview);
    }

    @Override
    @Transactional
    public InterviewDto cancelInterview(Long id) {

        Interview interview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        if (interview.getStatus() == Status.COMPLETED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Completed interview cannot be cancelled"
            );
        }

        if (interview.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(interview.getCalendarEventId());
        }

        interview.setStatus(Status.CANCELLED);

        Interview savedInterview =
                interviewRepository.save(interview);

        interviewEventProducer.publishCancelled(
                savedInterview,
                "Interview cancelled"
        );

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("INTERVIEW")
                        .entityId(savedInterview.getInterviewId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", "CANCELLED"
                        ))
                        .serviceName("interview-service")
                        .endpoint("/api/v1/interviews/" + id + "/cancel")
                        .build()
        );

        return InterviewMapper.entityToDto(savedInterview);
    }

    @Override
    @Transactional
    public InterviewDto completeInterview(Long id) {

        Interview interview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        if (interview.getStatus() == Status.CANCELLED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Cancelled interview cannot be completed"
            );
        }

        interview.setStatus(Status.COMPLETED);

        Interview savedInterview =
                interviewRepository.save(interview);

        interviewEventProducer.publishCompleted(savedInterview);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("INTERVIEW")
                        .entityId(savedInterview.getInterviewId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "status", "COMPLETED"
                        ))
                        .serviceName("interview-service")
                        .endpoint("/api/v1/interviews/" + id + "/complete")
                        .build()
        );

        return InterviewMapper.entityToDto(savedInterview);
    }

    @Override
    @Transactional
    public void deleteInterview(Long id) {

        Interview interview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        interviewRepository.delete(interview);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("INTERVIEW")
                        .entityId(interview.getInterviewId())
                        .action(AuditAction.DELETE)
                        .serviceName("interview-service")
                        .endpoint("/api/v1/interviews/" + id)
                        .build()
        );


    }

    @Override
    @Transactional
    public List<Interview> getInterviewsForEmployee(Long employeeId) {

        List<Interviewer> assignments =
                interviewerRepository.findByEmployeeId(employeeId);

        return assignments.stream()
                .map(Interviewer::getInterview)
                .toList();
    }

    private void attachInterviewersToInterview(Interview interview) {

        if (interview == null || interview.getInterviewers() == null) {
            return;
        }

        for (Interviewer interviewer : interview.getInterviewers()) {
            interviewer.setInterview(interview);
        }
    }

    private void replaceInterviewers(
            Interview interview,
            List<Interviewer> newInterviewers
    ) {

        if (interview == null || newInterviewers == null) {
            return;
        }

        if (interview.getInterviewers() == null) {
            interview.setInterviewers(new ArrayList<>());
        } else {
            interview.getInterviewers().clear();
        }

        for (Interviewer interviewer : newInterviewers) {
            interviewer.setInterview(interview);
            interview.getInterviewers().add(interviewer);
        }
    }

    private String buildCandidateName(CandidateDto candidate) {

        if (candidate == null) {
            return "Candidate";
        }

        String firstName =
                candidate.getFirstName() != null
                        ? candidate.getFirstName()
                        : "";

        String lastName =
                candidate.getLastName() != null
                        ? candidate.getLastName()
                        : "";

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "Candidate" : fullName;
    }


}