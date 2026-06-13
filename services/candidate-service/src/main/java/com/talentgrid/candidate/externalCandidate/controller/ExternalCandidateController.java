package com.talentgrid.candidate.externalCandidate.controller;

import com.talentgrid.candidate.externalCandidate.dto.response.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.service.ExternalCandidateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/external-candidates")
public class ExternalCandidateController {

    private final ExternalCandidateService externalCandidateService;

    public ExternalCandidateController(
            ExternalCandidateService externalCandidateService
    ) {
        this.externalCandidateService = externalCandidateService;
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidate(
            @Valid @RequestBody ExternalCandidateDto externalCandidateDto
    ) {

        CandidateResponse response =
                externalCandidateService.createCandidate(externalCandidateDto);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    @PutMapping("/{candidateId}")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody ExternalCandidateDto externalCandidateDto
    ) {

        CandidateResponse response =
                externalCandidateService.updateCandidate(
                        candidateId,
                        externalCandidateDto
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<ExternalCandidateDto> getCandidateById(
            @PathVariable Long candidateId
    ) {

        ExternalCandidateDto response =
                externalCandidateService.getByCandidateId(candidateId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable Long candidateId
    ) {

        externalCandidateService.deleteById(candidateId);
        return ResponseEntity.noContent().build();
    }
}