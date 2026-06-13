package com.talentgrid.interview.interview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageMoveRequest {

    private String targetStage;
    private String reason;
}