package com.talentgrid.interview.interview.service;

import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InterviewService {

    InterviewDto createInterview(InterviewDto interviewDto);

    InterviewDto getInterviewById(Long id);

    Page<InterviewDto> getAllInterviews(
            Long applicationId,
            Status status,
            Type interviewType,
            Pageable pageable
    );

    InterviewDto updateInterview(
            Long id,
            InterviewDto interviewDto
    );

    InterviewDto cancelInterview(Long id);

    InterviewDto completeInterview(Long id);

    void deleteInterview(Long id);
}