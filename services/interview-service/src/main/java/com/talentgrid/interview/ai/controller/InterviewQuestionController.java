//package com.talentgrid.interview.ai.controller;
//
//import com.talentgrid.interview.ai.dto.FollowUpQuestionsResponseDto;
//import com.talentgrid.interview.ai.service.InterviewQuestionService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//// @RestController
//// @RequestMapping("/api")
//// @RequiredArgsConstructor
//public class InterviewQuestionController {
//
//  //  private final InterviewQuestionService interviewQuestionService;
//
//    /**
//     * REQ-ER-14: Generate 5–8 AI-suggested follow-up interview questions
//     * based on the candidate's resume content and prior scorecard competency gaps.
//     * Each question is labelled with a clear intent (e.g. "Gap Probe – Technical").
//     *
//     * GET /api/interviews/{interviewId}/follow-up-questions
//     */
//    // @PreAuthorize("hasAuthority('INTERVIEW_VIEW')")
//    // @GetMapping("/interviews/{interviewId}/follow-up-questions")
//    // public ResponseEntity<FollowUpQuestionsResponseDto> getFollowUpQuestions(
//    //         @PathVariable Long interviewId) {
//    //     FollowUpQuestionsResponseDto response = interviewQuestionService.generateFollowUpQuestions(interviewId);
//
//    //     return ResponseEntity.ok(response);
//    }
//}
