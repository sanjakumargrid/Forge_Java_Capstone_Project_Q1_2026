package com.talentgrid.interview.interview.mapper;

import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.entity.Interview;

import java.util.ArrayList;

public class InterviewMapper {

    private InterviewMapper() {
    }

    public static Interview dtoToEntity(InterviewDto dto) {

        if (dto == null) {
            return null;
        }

        Interview entity = new Interview();

        entity.setInterviewId(dto.getId());
        entity.setApplicationId(dto.getApplicationId());
        entity.setInterviewers(dto.getInterviewers());
        entity.setInterviewType(dto.getInterviewType());
        entity.setScheduledAt(dto.getScheduledAt());
        entity.setDurationMins(dto.getDurationMins());
        entity.setTimeZone(dto.getTimeZone());
        entity.setMeetLink(dto.getMeetLink());
        entity.setStatus(dto.getStatus());
        entity.setCalendarEventId(dto.getCalendarEventId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }

    public static InterviewDto entityToDto(Interview entity) {

        if (entity == null) {
            return null;
        }

        InterviewDto dto = new InterviewDto();

        dto.setId(entity.getInterviewId());
        dto.setApplicationId(entity.getApplicationId());
        dto.setInterviewers(entity.getInterviewers());
        dto.setInterviewType(entity.getInterviewType());
        dto.setScheduledAt(entity.getScheduledAt());
        dto.setDurationMins(entity.getDurationMins());
        dto.setTimeZone(entity.getTimeZone());
        dto.setMeetLink(entity.getMeetLink());
        dto.setStatus(entity.getStatus());
        dto.setCalendarEventId(entity.getCalendarEventId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setScorecardIds(new ArrayList<>());

        return dto;
    }
}