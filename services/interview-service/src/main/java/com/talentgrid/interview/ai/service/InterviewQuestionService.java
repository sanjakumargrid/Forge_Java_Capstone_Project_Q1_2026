package com.talentgrid.interview.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.interview.ai.dto.FollowUpQuestionDto;
import com.talentgrid.interview.ai.dto.FollowUpQuestionsResponseDto;
import com.talentgrid.interview.client.ApplicationClient;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.dto.ApplicationDto;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.interview.repository.InterviewRepository;
import com.talentgrid.interview.scorecard.entity.Scorecard;
import com.talentgrid.interview.scorecard.repository.ScorecardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private final InterviewRepository interviewRepository;
    private final ScorecardRepository scorecardRepository;
    private final ApplicationClient applicationClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generates 5–8 targeted follow-up interview questions using Gemini AI,
     * derived from the candidate's resume content and competency gaps identified
     * in prior scorecards for the same application.
     *
     * @param interviewId the ID of the upcoming interview to generate questions for
     * @return FollowUpQuestionsResponseDto containing labelled follow-up questions
     */
    public FollowUpQuestionsResponseDto generateFollowUpQuestions(Long interviewId) {

        // 1. Load interview
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Interview not found with id: " + interviewId
                ));

        Long applicationId = interview.getApplicationId();

        // 2. Load application data (resume info + AI-matched/missing skills)
        ApplicationDto application = applicationClient.getApplication(applicationId);

        // 3. Load all prior scorecards for this application (cross-round gap analysis)
        List<Scorecard> priorScorecards =
                scorecardRepository.findAllByApplicationId(applicationId);

        // 4. Build prompt
        String prompt = buildPrompt(application, priorScorecards, interview);

        // 5. Call Gemini API
        String rawGeminiResponse = callGeminiApi(prompt);

        // 6. Parse Gemini response → list of FollowUpQuestionDto
        List<FollowUpQuestionDto> questions = parseGeminiResponse(rawGeminiResponse);

        return new FollowUpQuestionsResponseDto(interviewId, applicationId, questions);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt Builder
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPrompt(
            ApplicationDto application,
            List<Scorecard> priorScorecards,
            Interview interview
    ) {
        StringBuilder sb = new StringBuilder();

        // --- Resume context ---
        sb.append("## Candidate Resume Context\n");

        if (application.getMatchedSkills() != null && !application.getMatchedSkills().isEmpty()) {
            sb.append("Matched Skills: ")
              .append(String.join(", ", application.getMatchedSkills()))
              .append("\n");
        }

        if (application.getMissingSkills() != null && !application.getMissingSkills().isEmpty()) {
            sb.append("Missing/Weak Skills (from AI screening): ")
              .append(String.join(", ", application.getMissingSkills()))
              .append("\n");
        }

        if (application.getAiRationale() != null && !application.getAiRationale().isBlank()) {
            sb.append("AI Screening Rationale: ")
              .append(application.getAiRationale())
              .append("\n");
        }

        if (application.getOtherSkills() != null && !application.getOtherSkills().isEmpty()) {
            sb.append("Other Candidate Skills: ")
              .append(String.join(", ", application.getOtherSkills()))
              .append("\n");
        }

        // --- Prior scorecard competency gaps ---
        if (!priorScorecards.isEmpty()) {
            sb.append("\n## Prior Scorecard Competency Scores (1–10 scale)\n");

            priorScorecards.forEach(sc -> {
                sb.append(String.format(
                        "- Round scorecard: Technical=%d, Communication=%d, ProblemSolving=%d, CultureFit=%d | Avg=%.1f | Recommendation=%s\n",
                        sc.getTechnicalScore(),
                        sc.getCommunicationScore(),
                        sc.getProblemSolvingScore(),
                        sc.getCultureFitScore(),
                        sc.getAverageScore(),
                        sc.getRecommendation()
                ));

                if (sc.getOverallFeedback() != null && !sc.getOverallFeedback().isBlank()) {
                    sb.append("  Interviewer Feedback: ").append(sc.getOverallFeedback()).append("\n");
                }
            });

            // Compute average competency gaps across all scorecards
            double avgTechnical = priorScorecards.stream()
                    .mapToInt(Scorecard::getTechnicalScore).average().orElse(0);
            double avgCommunication = priorScorecards.stream()
                    .mapToInt(Scorecard::getCommunicationScore).average().orElse(0);
            double avgProblemSolving = priorScorecards.stream()
                    .mapToInt(Scorecard::getProblemSolvingScore).average().orElse(0);
            double avgCultureFit = priorScorecards.stream()
                    .mapToInt(Scorecard::getCultureFitScore).average().orElse(0);

            sb.append(String.format(
                    "\nAggregate average competency scores — Technical: %.1f, Communication: %.1f, Problem Solving: %.1f, Culture Fit: %.1f\n",
                    avgTechnical, avgCommunication, avgProblemSolving, avgCultureFit
            ));
        } else {
            sb.append("\n## Prior Scorecard Competency Scores\n");
            sb.append("No prior scorecards available. This is the first interview round.\n");
        }

        // --- Interview type context ---
        sb.append("\n## Upcoming Interview Details\n");
        sb.append("Interview Type: ").append(interview.getInterviewType()).append("\n");

        // --- Instruction ---
        sb.append("""

## Task
You are an expert technical recruiter and interview coach.
Based on the candidate's resume context and the competency gaps identified from prior scorecard rounds above,
generate exactly 6 targeted follow-up interview questions for the upcoming interview round.

Each question must:
1. Be specific and directly tied to either a resume detail or a scorecard competency gap.
2. Have a clear intent label. Intent labels must be one of:
   - "Gap Probe – Technical"
   - "Gap Probe – Communication"
   - "Gap Probe – Problem Solving"
   - "Gap Probe – Culture Fit"
   - "Resume Deep-Dive – Experience"
   - "Resume Deep-Dive – Skills"
   - "Behavioural – Situational"
   - "Behavioural – Leadership"

Return ONLY a valid JSON array. No markdown, no explanation, no surrounding text. Example format:
[
  { "question": "...", "intent": "Gap Probe – Technical" },
  { "question": "...", "intent": "Resume Deep-Dive – Experience" }
]
""");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gemini API Call
    // ─────────────────────────────────────────────────────────────────────────

    private String callGeminiApi(String prompt) {
        String url = GEMINI_URL + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("[InterviewQuestionService] Calling Gemini API to generate follow-up questions");

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini API returned an unsuccessful response: " + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Response Parser
    // ─────────────────────────────────────────────────────────────────────────

    private List<FollowUpQuestionDto> parseGeminiResponse(String geminiResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(geminiResponseBody);

            // Navigate: candidates[0].content.parts[0].text
            String jsonArrayText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            return objectMapper.readValue(
                    jsonArrayText,
                    new TypeReference<List<FollowUpQuestionDto>>() {}
            );

        } catch (Exception e) {
            log.error("[InterviewQuestionService] Failed to parse Gemini response: {}", e.getMessage());
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse AI response. Please try again."
            );
        }
    }
}
