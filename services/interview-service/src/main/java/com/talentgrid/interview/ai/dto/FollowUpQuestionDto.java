package com.talentgrid.interview.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FollowUpQuestionDto {

    /**
     * The follow-up interview question text.
     */
    private String question;

    /**
     * The intent label explaining why this question is being asked,
     * e.g. "Gap Probe – Technical", "Resume Deep-Dive – Experience", etc.
     */
    private String intent;
}
