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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

        private final InterviewRepository interviewRepository;
        private final ScorecardRepository scorecardRepository;
        private final ApplicationClient applicationClient;
        private final ObjectMapper objectMapper;

        @Value("${gemini.enabled:false}")
        private boolean geminiEnabled;

        @Value("${gemini.api.key:dummy-local-key}")
        private String geminiApiKey;

        @Value("${gemini.model:gemini-2.0-flash}")
        private String geminiModel;

        @Value("${gemini.fail-fast:false}")
        private boolean geminiFailFast;

        private final RestTemplate restTemplate = new RestTemplate();

        public FollowUpQuestionsResponseDto generateFollowUpQuestions(Long interviewId) {

                Interview interview = interviewRepository.findById(interviewId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Interview not found with id: " + interviewId));

                Long applicationId = interview.getApplicationId();

                ApplicationDto application = applicationClient.getApplication(applicationId);

                List<Scorecard> priorScorecards = scorecardRepository.findAllByApplicationId(applicationId);

                String prompt = buildPrompt(application, priorScorecards, interview);

                List<FollowUpQuestionDto> questions;

                if (!isGeminiUsable()) {
                        log.warn("[InterviewQuestionService] Gemini disabled or API key missing. Returning local mock follow-up questions.");
                        questions = buildLocalFallbackQuestions();
                } else {
                        try {
                                String rawGeminiResponse = callGeminiApi(prompt);
                                questions = parseGeminiResponse(rawGeminiResponse);

                                if (questions == null || questions.size() < 5) {
                                        log.warn("[InterviewQuestionService] Gemini returned less than 5 questions. Using local fallback.");
                                        questions = buildLocalFallbackQuestions();
                                }

                                if (questions.size() > 8) {
                                        questions = questions.subList(0, 8);
                                }

                        } catch (RestClientException ex) {
                                log.error("[InterviewQuestionService] Gemini API call failed: {}", ex.getMessage());

                                if (geminiFailFast) {
                                        throw new BusinessException(
                                                        HttpStatus.BAD_GATEWAY,
                                                        "Gemini API failed while generating interview questions.");
                                }

                                questions = buildLocalFallbackQuestions();

                        } catch (Exception ex) {
                                log.error("[InterviewQuestionService] Failed to generate AI questions: {}",
                                                ex.getMessage());

                                if (geminiFailFast) {
                                        throw new BusinessException(
                                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                                        "Failed to generate interview questions.");
                                }

                                questions = buildLocalFallbackQuestions();
                        }
                }

                return new FollowUpQuestionsResponseDto(interviewId, applicationId, questions);
        }

        private boolean isGeminiUsable() {
                return geminiEnabled
                                && geminiApiKey != null
                                && !geminiApiKey.isBlank()
                                && !"dummy-local-key".equalsIgnoreCase(geminiApiKey);
        }

        private String buildPrompt(
                        ApplicationDto application,
                        List<Scorecard> priorScorecards,
                        Interview interview) {
                StringBuilder sb = new StringBuilder();

                sb.append("## Candidate Resume Context\n");

                if (application.getMatchedSkills() != null && !application.getMatchedSkills().isEmpty()) {
                        sb.append("Matched Skills: ")
                                        .append(String.join(", ", application.getMatchedSkills()))
                                        .append("\n");
                }

                if (application.getMissingSkills() != null && !application.getMissingSkills().isEmpty()) {
                        sb.append("Missing/Weak Skills from screening: ")
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

                if (priorScorecards != null && !priorScorecards.isEmpty()) {
                        sb.append("\n## Prior Scorecard Competency Scores\n");

                        for (Scorecard sc : priorScorecards) {
                                sb.append(String.format(
                                                "- Technical=%d, Communication=%d, ProblemSolving=%d, CultureFit=%d, Avg=%.1f, Recommendation=%s\n",
                                                sc.getTechnicalScore(),
                                                sc.getCommunicationScore(),
                                                sc.getProblemSolvingScore(),
                                                sc.getCultureFitScore(),
                                                sc.getAverageScore(),
                                                sc.getRecommendation()));

                                if (sc.getOverallFeedback() != null && !sc.getOverallFeedback().isBlank()) {
                                        sb.append("  Feedback: ")
                                                        .append(sc.getOverallFeedback())
                                                        .append("\n");
                                }
                        }

                        double avgTechnical = priorScorecards.stream()
                                        .mapToInt(Scorecard::getTechnicalScore)
                                        .average()
                                        .orElse(0);

                        double avgCommunication = priorScorecards.stream()
                                        .mapToInt(Scorecard::getCommunicationScore)
                                        .average()
                                        .orElse(0);

                        double avgProblemSolving = priorScorecards.stream()
                                        .mapToInt(Scorecard::getProblemSolvingScore)
                                        .average()
                                        .orElse(0);

                        double avgCultureFit = priorScorecards.stream()
                                        .mapToInt(Scorecard::getCultureFitScore)
                                        .average()
                                        .orElse(0);

                        sb.append(String.format(
                                        "\nAggregate scores: Technical=%.1f, Communication=%.1f, ProblemSolving=%.1f, CultureFit=%.1f\n",
                                        avgTechnical,
                                        avgCommunication,
                                        avgProblemSolving,
                                        avgCultureFit));
                } else {
                        sb.append("\n## Prior Scorecard Competency Scores\n");
                        sb.append("No prior scorecards available.\n");
                }

                sb.append("\n## Upcoming Interview Details\n");
                sb.append("Interview Type: ")
                                .append(interview.getInterviewType())
                                .append("\n");

                sb.append("""

                                ## Task
                                You are an expert technical recruiter and interview coach.

                                Generate exactly 6 targeted follow-up interview questions.

                                Each question must:
                                1. Be specific and directly tied to resume skills, missing skills, or scorecard competency gaps.
                                2. Include a clear intent label.
                                3. Use only one of these intent labels:
                                   - Gap Probe – Technical
                                   - Gap Probe – Communication
                                   - Gap Probe – Problem Solving
                                   - Gap Probe – Culture Fit
                                   - Resume Deep-Dive – Experience
                                   - Resume Deep-Dive – Skills
                                   - Behavioural – Situational
                                   - Behavioural – Leadership

                                Return ONLY valid JSON array.
                                No markdown.
                                No explanation.
                                No surrounding text.

                                Example:
                                [
                                  { "question": "Can you explain how you used Spring Boot in your previous project?", "intent": "Resume Deep-Dive – Skills" },
                                  { "question": "Describe a time when you solved a production issue under pressure.", "intent": "Behavioural – Situational" }
                                ]
                                """);

                return sb.toString();
        }

        private String callGeminiApi(String prompt) {
                String url = UriComponentsBuilder
                                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent")
                                .queryParam("key", geminiApiKey)
                                .buildAndExpand(geminiModel)
                                .toUriString();

                Map<String, Object> requestBody = Map.of(
                                "contents", List.of(
                                                Map.of(
                                                                "role", "user",
                                                                "parts", List.of(
                                                                                Map.of("text", prompt)))),
                                "generationConfig", Map.of(
                                                "responseMimeType", "application/json",
                                                "temperature", 0.5,
                                                "maxOutputTokens", 2048));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                log.info("[InterviewQuestionService] Calling Gemini model: {}", geminiModel);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new BusinessException(
                                        HttpStatus.BAD_GATEWAY,
                                        "Gemini API returned unsuccessful response: " + response.getStatusCode());
                }

                return response.getBody();
        }

        private List<FollowUpQuestionDto> parseGeminiResponse(String geminiResponseBody) {
                try {
                        JsonNode root = objectMapper.readTree(geminiResponseBody);

                        JsonNode candidates = root.path("candidates");

                        if (!candidates.isArray() || candidates.isEmpty()) {
                                throw new IllegalStateException("Gemini response does not contain candidates");
                        }

                        JsonNode parts = candidates.get(0)
                                        .path("content")
                                        .path("parts");

                        if (!parts.isArray() || parts.isEmpty()) {
                                throw new IllegalStateException("Gemini response does not contain content parts");
                        }

                        String jsonArrayText = parts.get(0)
                                        .path("text")
                                        .asText();

                        String cleanedJson = cleanJsonArrayText(jsonArrayText);

                        return objectMapper.readValue(
                                        cleanedJson,
                                        new TypeReference<List<FollowUpQuestionDto>>() {
                                        });

                } catch (Exception ex) {
                        log.error("[InterviewQuestionService] Failed to parse Gemini response: {}", ex.getMessage());
                        throw new BusinessException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Failed to parse AI response.");
                }
        }

        private String cleanJsonArrayText(String text) {
                if (text == null || text.isBlank()) {
                        throw new IllegalArgumentException("Gemini response text is empty");
                }

                String cleaned = text.trim();

                if (cleaned.startsWith("```")) {
                        cleaned = cleaned
                                        .replaceFirst("^```json\\s*", "")
                                        .replaceFirst("^```\\s*", "")
                                        .replaceFirst("\\s*```$", "")
                                        .trim();
                }

                int start = cleaned.indexOf("[");
                int end = cleaned.lastIndexOf("]");

                if (start == -1 || end == -1 || end <= start) {
                        throw new IllegalArgumentException("No valid JSON array found in Gemini response");
                }

                return cleaned.substring(start, end + 1);
        }

        private List<FollowUpQuestionDto> buildLocalFallbackQuestions() {
                try {
                        String fallbackJson = """
                                        [
                                          {
                                            "question": "Can you explain one project where you used Java and Spring Boot to build a backend API?",
                                            "intent": "Resume Deep-Dive – Skills"
                                          },
                                          {
                                            "question": "How did you handle database operations and query optimization in your previous work?",
                                            "intent": "Resume Deep-Dive – Experience"
                                          },
                                          {
                                            "question": "Can you describe a difficult technical problem you solved and how you approached it?",
                                            "intent": "Gap Probe – Problem Solving"
                                          },
                                          {
                                            "question": "How do you explain technical issues to a non-technical team member or stakeholder?",
                                            "intent": "Gap Probe – Communication"
                                          },
                                          {
                                            "question": "Tell me about a time when you received feedback and improved your work based on it.",
                                            "intent": "Behavioural – Situational"
                                          },
                                          {
                                            "question": "How do you make sure your code is maintainable, testable, and easy for others to understand?",
                                            "intent": "Gap Probe – Technical"
                                          }
                                        ]
                                        """;

                        return objectMapper.readValue(
                                        fallbackJson,
                                        new TypeReference<List<FollowUpQuestionDto>>() {
                                        });

                } catch (Exception ex) {
                        throw new BusinessException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Failed to build fallback interview questions.");
                }
        }
}