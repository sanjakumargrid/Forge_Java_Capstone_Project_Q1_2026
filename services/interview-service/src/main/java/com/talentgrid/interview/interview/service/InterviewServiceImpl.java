package com.talentgrid.interview.interview.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
import com.talentgrid.interview.client.ApplicationClient;
import com.talentgrid.interview.client.CandidateClient;
import com.talentgrid.interview.client.EmployeeClient;
import com.talentgrid.interview.client.dto.CandidateDto;
import com.talentgrid.interview.client.dto.EmployeeDto;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.interview.interview.dto.ApplicationDto;
import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import com.talentgrid.interview.interview.integration.GoogleCalendarClient;
import com.talentgrid.interview.interview.integration.GoogleCalendarResponse;
import com.talentgrid.interview.interview.mapper.InterviewMapper;
import com.talentgrid.interview.interview.repository.InterviewRepository;
import com.talentgrid.interview.kafka.producer.InterviewEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;

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

        Long applicationId = interviewDto.getApplicationId();

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

        // REQ: Fetch Candidate early to avoid orphaned Calendar events if candidate fetch fails
        if (applicationDto.getCandidateId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Application does not have an associated candidate");
        }
        CandidateDto candidate = candidateClient.getCandidate(applicationDto.getCandidateId());

        Interview interview =
                InterviewMapper.dtoToEntity(interviewDto);

        if (interview.getStatus() == null) {
            interview.setStatus(Status.SCHEDULED);
        }

        GoogleCalendarResponse response =
                googleCalendarClient.createEvent(interview);

        interview.setCalendarEventId(response.getEventId());
        interview.setMeetLink(response.getMeetLink());

        Interview savedInterview =
                interviewRepository.save(interview);
        
        String interviewerName = "Our Team";
        if (interview.getInterviewers() != null && !interview.getInterviewers().isEmpty()) {
            EmployeeDto primaryInterviewer = employeeClient.getEmployee(interview.getInterviewers().get(0));
            interviewerName = primaryInterviewer.getName();
        }

        String candidateName = candidate.getFirstName() + (candidate.getLastName() != null ? " " + candidate.getLastName() : "");
        
        // Send email to Candidate
        notificationEventPublisher.sendInAppAndEmail(
                applicationDto.getCandidateId().toString(),
                candidate.getEmail(),
                "INTERVIEW_INVITATION",
                "Interview Invitation from Grid Dynamics",
                "You have been invited to an interview. Please join using the Google Meet link: " + response.getMeetLink(),
                "interview-service",
                savedInterview.getInterviewId().toString(),
                "INTERVIEW",
                "HIGH",
                "interview-invitation",
                Map.of(
                        "candidateName", candidateName,
                        "companyName", "Grid Dynamics",
                        "interviewDate", savedInterview.getScheduledAt() != null ? savedInterview.getScheduledAt().toString() : "TBD",
                        "meetLink", response.getMeetLink() != null ? response.getMeetLink() : "TBD",
                        "interviewerName", interviewerName
                ),
                java.util.UUID.randomUUID().toString()
        );

        // Send emails to all Interviewers
        if (interview.getInterviewers() != null) {
            for (Long interviewerId : interview.getInterviewers()) {
                try {
                    EmployeeDto employee = employeeClient.getEmployee(interviewerId);
                    if (employee != null && employee.getEmail() != null) {
                        notificationEventPublisher.sendInAppAndEmail(
                                String.valueOf(interviewerId),
                                employee.getEmail(),
                                "INTERVIEW_INVITATION",
                                "Interview Scheduled: " + candidateName,
                                "You have been scheduled to interview " + candidateName + ". Please join using the Google Meet link: " + response.getMeetLink(),
                                "interview-service",
                                savedInterview.getInterviewId().toString(),
                                "INTERVIEW",
                                "HIGH",
                                "interview-invitation",
                                Map.of(
                                        "candidateName", candidateName,
                                        "companyName", "Grid Dynamics",
                                        "interviewDate", savedInterview.getScheduledAt() != null ? savedInterview.getScheduledAt().toString() : "TBD",
                                        "meetLink", response.getMeetLink() != null ? response.getMeetLink() : "TBD",
                                        "interviewerName", employee.getName()
                                ),
                                java.util.UUID.randomUUID().toString()
                        );
                    }
                } catch (Exception e) {
                    // Log error but don't fail the interview creation
                    System.err.println("Failed to send email to interviewer " + interviewerId + ": " + e.getMessage());
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
                                "status", savedInterview.getStatus().name()
                        ))
                        .serviceName("interview-service")
                        .endpoint("/api/interviews")
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
            Pageable pageable
    ) {

        Page<Interview> interviews;

        if (applicationId != null && status != null && interviewType != null) {
            interviews = interviewRepository.findByApplicationIdAndStatusAndInterviewType(
                    applicationId,
                    status,
                    interviewType,
                    pageable
            );
        } else if (applicationId != null && status != null) {
            interviews = interviewRepository.findByApplicationIdAndStatus(
                    applicationId,
                    status,
                    pageable
            );
        } else if (applicationId != null && interviewType != null) {
            interviews = interviewRepository.findByApplicationIdAndInterviewType(
                    applicationId,
                    interviewType,
                    pageable
            );
        } else if (status != null && interviewType != null) {
            interviews = interviewRepository.findByStatusAndInterviewType(
                    status,
                    interviewType,
                    pageable
            );
        } else if (applicationId != null) {
            interviews = interviewRepository.findByApplicationId(
                    applicationId,
                    pageable
            );
        } else if (status != null) {
            interviews = interviewRepository.findByStatus(
                    status,
                    pageable
            );
        } else if (interviewType != null) {
            interviews = interviewRepository.findByInterviewType(
                    interviewType,
                    pageable
            );
        } else {
            interviews = interviewRepository.findAll(pageable);
        }

        return interviews.map(InterviewMapper::entityToDto);
    }

    @Override
    @Transactional
    public InterviewDto updateInterview(
            Long id,
            InterviewDto interviewDto
    ) {

        Interview existingInterview =
                interviewRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview not found with id: " + id
                                )
                        );

        if (interviewDto.getApplicationId() != null
                && !interviewDto.getApplicationId().equals(existingInterview.getApplicationId())) {

            ApplicationDto applicationDto =
                    applicationClient.getApplication(interviewDto.getApplicationId());

            if (applicationDto == null || applicationDto.getApplicationId() == null) {
                throw new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Application not found with id: " + interviewDto.getApplicationId()
                );
            }

            existingInterview.setApplicationId(interviewDto.getApplicationId());
        }

        existingInterview.setInterviewers(interviewDto.getInterviewers());
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
                        .endpoint("/api/interviews/" + id)
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
                        .endpoint("/api/interviews/" + id + "/cancel")
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
                        .endpoint("/api/interviews/" + id + "/complete")
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
                        .endpoint("/api/interviews/" + id)
                        .build()
        );
    }
}