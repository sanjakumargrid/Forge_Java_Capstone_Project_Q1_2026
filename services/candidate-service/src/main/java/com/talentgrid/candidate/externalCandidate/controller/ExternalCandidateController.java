//package com.talentgrid.candidate.externalCandidate.controller;
//
//import com.talentgrid.candidate.externalCandidate.dto.response.CandidateResponse;
//import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
//import com.talentgrid.candidate.externalCandidate.service.ExternalCandidateService;
//import jakarta.validation.Valid;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/external-candidates")
//public class ExternalCandidateController {
//
//    private final ExternalCandidateService externalCandidateService;
//
//    public ExternalCandidateController(
//            ExternalCandidateService externalCandidateService
//    ) {
//        this.externalCandidateService = externalCandidateService;
//    }
//    @PreAuthorize("hasAuthority('CANDIDATE_CREATE')")
//    @PostMapping
//    public ResponseEntity<CandidateResponse> createCandidate(
//            @Valid @RequestBody ExternalCandidateDto externalCandidateDto
//    ) {
//
//        CandidateResponse response =
//                externalCandidateService.createCandidate(externalCandidateDto);
//
//        return ResponseEntity
//                .status(response.getStatus())
//                .body(response);
//    }
//    @PreAuthorize("hasAuthority('CANDIDATE_UPDATE')")
//    @PutMapping("/{candidateId}")
//    public ResponseEntity<CandidateResponse> updateCandidate(
//            @PathVariable Long candidateId,
//            @Valid @RequestBody ExternalCandidateDto externalCandidateDto
//    ) {
//
//        CandidateResponse response =
//                externalCandidateService.updateCandidate(
//                        candidateId,
//                        externalCandidateDto
//                );
//
//        return ResponseEntity.ok(response);
//    }
//    @PreAuthorize("hasAuthority('CANDIDATE_VIEW')")
//    @GetMapping("/{candidateId}")
//    public ResponseEntity<ExternalCandidateDto> getCandidateById(
//            @PathVariable Long candidateId
//    ) {
//
//        ExternalCandidateDto response =
//                externalCandidateService.getByCandidateId(candidateId);
//
//        return ResponseEntity.ok(response);
//    }
//    @PreAuthorize("hasAuthority('CANDIDATE_DELETE')")
//    @DeleteMapping("/{candidateId}")
//    public ResponseEntity<Void> deleteById(
//            @PathVariable Long candidateId
//    ) {
//
//        externalCandidateService.deleteById(candidateId);
//        return ResponseEntity.noContent().build();
//    }
//}

package com.talentgrid.candidate.externalCandidate.controller;

import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.dto.response.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.service.ExternalCandidateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/external-candidates")
public class ExternalCandidateController {

    private final ExternalCandidateService externalCandidateService;

    public ExternalCandidateController(ExternalCandidateService externalCandidateService) {
        this.externalCandidateService = externalCandidateService;
    }

    // PUBLIC - external candidate apply/create
    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidate(
            @Valid @RequestBody ExternalCandidateDto request
    ) {
        CandidateResponse response = externalCandidateService.createCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUBLIC INTERNAL - used by application-service during public apply flow
    @GetMapping("/internal/{candidateId}")
    public ResponseEntity<ExternalCandidateDto> getCandidateByIdForInternal(
            @PathVariable Long candidateId
    ) {
        ExternalCandidateDto response =
                externalCandidateService.getByCandidateId(candidateId);

        return ResponseEntity.ok(response);
    }

    // SECURED - recruiter/admin update
    @PreAuthorize("hasAuthority('CANDIDATE_UPDATE')")
    @PutMapping("/{candidateId}")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody ExternalCandidateDto externalCandidateDto
    ) {
        CandidateResponse response =
                externalCandidateService.updateCandidate(candidateId, externalCandidateDto);

        return ResponseEntity.ok(response);
    }

    // SECURED - recruiter/admin view
    @PreAuthorize("hasAuthority('CANDIDATE_VIEW')")
    @GetMapping("/{candidateId}")
    public ResponseEntity<ExternalCandidateDto> getCandidateById(
            @PathVariable Long candidateId
    ) {
        ExternalCandidateDto response =
                externalCandidateService.getByCandidateId(candidateId);

        return ResponseEntity.ok(response);
    }

    // SECURED - recruiter/admin delete
    @PreAuthorize("hasAuthority('CANDIDATE_DELETE')")
    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable Long candidateId
    ) {
        externalCandidateService.deleteById(candidateId);
        return ResponseEntity.noContent().build();
    }
}