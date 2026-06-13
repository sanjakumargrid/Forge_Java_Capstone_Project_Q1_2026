package com.talentgrid.interview.scorecard.controller;

import com.talentgrid.interview.scorecard.dto.ScorecardRequestDto;
import com.talentgrid.interview.scorecard.dto.ScorecardResponseDto;
import com.talentgrid.interview.scorecard.dto.ScorecardSummaryDto;
import com.talentgrid.interview.scorecard.service.ScorecardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScorecardController {

    private final ScorecardService scorecardService;

    @PostMapping("/interviews/{interviewId}/scorecards")
    public ResponseEntity<ScorecardResponseDto> submitScorecard(
            @PathVariable Long interviewId,
            @Valid @RequestBody ScorecardRequestDto requestDto
    ) {

        ScorecardResponseDto response =
                scorecardService.submitScorecard(
                        interviewId,
                        requestDto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/scorecards/{scorecardId}")
    public ResponseEntity<ScorecardResponseDto> getScorecardById(
            @PathVariable Long scorecardId
    ) {

        ScorecardResponseDto response =
                scorecardService.getScorecardById(scorecardId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/interviews/{interviewId}/scorecards")
    public ResponseEntity<List<ScorecardSummaryDto>> getScorecardsByInterview(
            @PathVariable Long interviewId
    ) {

        List<ScorecardSummaryDto> response =
                scorecardService.getScorecardsByInterview(interviewId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications/{applicationId}/scorecards")
    public ResponseEntity<List<ScorecardSummaryDto>> getScorecardsByApplication(
            @PathVariable Long applicationId
    ) {

        List<ScorecardSummaryDto> response =
                scorecardService.getScorecardsByApplication(applicationId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/interviews/{interviewId}/scorecards/interviewer/{interviewerId}")
    public ResponseEntity<ScorecardResponseDto> getScorecardByInterviewer(
            @PathVariable Long interviewId,
            @PathVariable Long interviewerId
    ) {

        ScorecardResponseDto response =
                scorecardService.getScorecardByInterviewer(
                        interviewId,
                        interviewerId
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/scorecards/{scorecardId}")
    public ResponseEntity<Void> deleteScorecard(
            @PathVariable Long scorecardId
    ) {

        scorecardService.deleteScorecard(scorecardId);

        return ResponseEntity.noContent().build();
    }
}