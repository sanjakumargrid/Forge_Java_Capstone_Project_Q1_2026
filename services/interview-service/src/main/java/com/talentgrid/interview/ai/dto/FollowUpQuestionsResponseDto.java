package com.talentgrid.interview.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FollowUpQuestionsResponseDto {

    private Long interviewId;

    private Long applicationId;

    /**
     * 5–8 AI-generated follow-up questions, each labeled with an intent.
     */
    private List<FollowUpQuestionDto> questions;
}
