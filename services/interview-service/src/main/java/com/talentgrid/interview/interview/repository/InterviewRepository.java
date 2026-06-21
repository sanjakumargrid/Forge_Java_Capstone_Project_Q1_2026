package com.talentgrid.interview.interview.repository;

import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Page<Interview> findByApplicationId(
            Long applicationId,
            Pageable pageable
    );

    Page<Interview> findByStatus(
            Status status,
            Pageable pageable
    );

    Page<Interview> findByApplicationIdAndStatus(
            Long applicationId,
            Status status,
            Pageable pageable
    );

    Page<Interview> findByInterviewType(
            Type interviewType,
            Pageable pageable
    );

    Page<Interview> findByApplicationIdAndInterviewType(
            Long applicationId,
            Type interviewType,
            Pageable pageable
    );

    Page<Interview> findByStatusAndInterviewType(
            Status status,
            Type interviewType,
            Pageable pageable
    );

    Page<Interview> findByApplicationIdAndStatusAndInterviewType(
            Long applicationId,
            Status status,
            Type interviewType,
            Pageable pageable
    );

    List<Interview> findByScheduledAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Interview> findByScheduledAtAfter(
            LocalDateTime dateTime
    );

    List<Interview> findByInterviewersContains(
            Long interviewerEmployeeId
    );

    long countByStatus(Status status);

    long countByApplicationId(Long applicationId);

    boolean existsByCalendarEventId(String calendarEventId);
}