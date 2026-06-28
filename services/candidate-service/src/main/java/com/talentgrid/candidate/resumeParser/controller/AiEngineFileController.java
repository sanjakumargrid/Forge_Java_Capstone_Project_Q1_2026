package com.talentgrid.candidate.resumeParser.controller;



import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.resumeParser.client.DemandServiceClient;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
import com.talentgrid.candidate.resumeParser.model.AtsEvaluationDTO;
import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import com.talentgrid.candidate.resumeParser.service.AtsEvaluationService;
import com.talentgrid.candidate.resumeParser.service.ResumeParserService;
import com.talentgrid.candidate.resumeParser.service.ResumeStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aiengine")
public class AiEngineFileController {

    private final ResumeParserService resumeParserService;
    private final DemandServiceClient demandServiceClient;
    private final AtsEvaluationService atsEvaluationService;
    private final ResumeStoreService resumeStoreService;

    // Notice we no longer need the RestTemplate or applicationServiceBaseUrl in this controller
    // because the frontend is handling the database save now.

    public AiEngineFileController(ResumeParserService resumeParserService,
                                  DemandServiceClient demandServiceClient,
                                  AtsEvaluationService atsEvaluationService,
                                  ResumeStoreService resumeStoreService) {
        this.resumeParserService = resumeParserService;
        this.demandServiceClient = demandServiceClient;
        this.atsEvaluationService = atsEvaluationService;
        this.resumeStoreService = resumeStoreService;
    }

    @PostMapping
    public ResponseEntity<String> saveResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File type not supported");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File name is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String response = resumeStoreService.saveResume(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseResume(@RequestParam("file") MultipartFile file) {
        try {
            List<String> allowedTypes = List.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
            );

            if (file.isEmpty() || file.getContentType() == null || !allowedTypes.contains(file.getContentType())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid file type. Please upload a PDF, DOCX, or TXT file.");
            }

            ParsedResumeDTO parsedResume = resumeParserService.parseResume(file);
            return ResponseEntity.ok(parsedResume);
        } catch (BusinessException be) {
            return ResponseEntity.status(be.getStatus()).body(Map.of("errors", be.getErrors()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage() != null ? e.getMessage() : e.toString()
            ));
        }
    }

    // Notice we REMOVED the applicationId parameter from the path and the inter-service call
    @PostMapping("/evaluate/{demandId}")
    public ResponseEntity<?> evaluate(
            @PathVariable Long demandId,
            @RequestParam("file") MultipartFile file) {

        try {
            List<String> allowedTypes = List.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
            );

            if (file.isEmpty() || file.getContentType() == null || !allowedTypes.contains(file.getContentType())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid file type. Please upload a PDF, DOCX, or TXT file.");
            }

            DemandDTO demand = demandServiceClient.fetchDemandById(demandId);

            if (demand == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "Demand not found.");
            }

            // The Service evaluates the resume and returns the DTO
            AtsEvaluationDTO evaluation = atsEvaluationService.evaluateResume(file, demand);

            // Return the Evaluation directly to the Frontend
            // The frontend will take this payload and hit your Application Service's POST endpoint to create the record.
            return ResponseEntity.ok(evaluation);

        } catch (BusinessException be) {
            return ResponseEntity.status(be.getStatus()).body(Map.of("errors", be.getErrors()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage() != null ? e.getMessage() : e.toString()
            ));
        }
    }
}