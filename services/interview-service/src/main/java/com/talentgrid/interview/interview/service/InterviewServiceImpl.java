package com.talentgrid.interview.interview.service;

import com.talentgrid.interview.client.ApplicationClient;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.dto.ApplicationDto;
import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import com.talentgrid.interview.interview.integration.GoogleCalendarClient;
import com.talentgrid.interview.interview.integration.GoogleCalendarResponse;
import com.talentgrid.interview.interview.mapper.InterviewMapper;
import com.talentgrid.interview.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;

    private final ApplicationClient applicationClient;

    private final GoogleCalendarClient googleCalendarClient;

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

        interview.setStatus(Status.CANCELLED);

        Interview savedInterview =
                interviewRepository.save(interview);

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
    }
}