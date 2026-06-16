package com.talentgrid.candidate.resumeParser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.candidate.resumeParser.client.DemandServiceClient;
import com.talentgrid.candidate.resumeParser.model.AtsEvaluationDTO;
import com.talentgrid.candidate.resumeParser.model.DemandDTO;
import com.talentgrid.candidate.resumeParser.model.ParsedResumeDTO;
import com.talentgrid.candidate.resumeParser.service.AtsEvaluationService;
import com.talentgrid.candidate.resumeParser.service.ResumeParserService;
import com.talentgrid.candidate.resumeParser.service.ResumeStoreService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;


@RestController
@RequestMapping("api/v1/aiengine")
public class AiEngineFileController {

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private DemandServiceClient demandServiceClient;

    @Autowired
    private AtsEvaluationService atsEvaluationService;


    final private ResumeStoreService resumeStoreService;

    @Autowired
    public AiEngineFileController(){
        this.resumeStoreService = new ResumeStoreService();
    }



    @PostMapping
    public ResponseEntity<String> saveResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        if (file.getContentType()==null||!file.getContentType().equals("application/pdf")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File type not supported");
        }

        String fileName = file.getOriginalFilename();
        if (fileName==null||fileName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File name is empty");
        }
        if(file.getSize()>10*1024*1024){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String response = resumeStoreService.saveResume(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseResume(@RequestParam("file") MultipartFile file) {
        try {

            ParsedResumeDTO parsedResume = resumeParserService.parseResume(file);
            return ResponseEntity.ok(parsedResume);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage() != null ? e.getMessage() : e.toString()
            ));
        }
    }

    @PostMapping("/evaluate/{demandId}")
    public ResponseEntity<?> evaluate(
            @PathVariable Long demandId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid PDF file."));
            }

            // 1. Fetch the Demand from the other microservice
            DemandDTO demand = demandServiceClient.fetchDemandById(demandId);

            if (demand == null || demand.getSkills() == null || demand.getSkills().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Demand not found or has no skills."));
            }

            // 2. Send the file and the List of Skills directly to the Evaluation Service
            AtsEvaluationDTO evaluation = atsEvaluationService.evaluateResume(file, demand.getSkills());

            return ResponseEntity.ok(evaluation);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage() != null ? e.getMessage() : e.toString()
            ));
        }
    }



}
