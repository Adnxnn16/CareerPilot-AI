package com.careerpilot.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * infrastructure/ai/OpenAIClient.java — NEW (F6 adds tailoring method)
 *
 * Handles all OpenAI API calls for CareerPilot.
 * - F2: resume parsing prompt (not implemented here yet — stub for integration)
 * - F4: match scoring prompt (not implemented here yet — stub for integration)
 * - F6: resume tailoring prompt (implemented below)
 *
 * All methods enforce JSON-only output via response_format: json_object.
 */
@Slf4j
@Component
public class OpenAIClient {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private static final String PARSE_SYSTEM_PROMPT = """
            You are an expert ATS parser. Extract information from the raw resume text.
            OUTPUT FORMAT: Return ONLY a single valid JSON object.
            
            OUTPUT CONTRACT — return exactly this JSON shape:
            {
              "skills": ["string"],
              "experience": [{"company":"string","title":"string","duration":"string","bullets":["string"]}]
            }
            """;

    private static final String MATCH_SYSTEM_PROMPT = """
            You are an expert technical recruiter evaluating a candidate's resume against a job description.
            Calculate a match score between 0 and 100. Provide reasoning.
            OUTPUT FORMAT: Return ONLY a single valid JSON object.

            OUTPUT CONTRACT — return exactly this JSON shape:
            {
              "matchScore": 85,
              "matchingSkills": ["string"],
              "missingSkills": ["string"],
              "reasoning": "string"
            }
            """;

    private static final String TAILORING_SYSTEM_PROMPT = """
            You are an expert ATS resume writer with deep knowledge of applicant tracking systems.
            Your task is to rewrite a candidate's existing resume to be optimally tailored for a specific job description.

            STRICT RULES — violating any rule makes your output invalid:
            1. OUTPUT FORMAT: Return ONLY a single valid JSON object. No markdown, no prose, no code fences.
            2. NO FABRICATION: You MUST NOT invent skills, experiences, companies, dates, job titles, certifications, \
            or educational credentials the candidate does not already possess. Every bullet point must be traceable \
            to the candidate's input data.
            3. HONEST KEYWORD MATCHING: Reorder and rephrase the candidate's existing bullet points to surface \
            keywords from the job description. You may change wording, tone, and emphasis — but never the underlying fact.
            4. UNMATCHED KEYWORDS: Any keyword from the job description that cannot be honestly mapped to the \
            candidate's existing experience must be listed in the "unmatchedKeywords" array. Do not force a match.
            5. ATS FORMAT: Use plain text bullet points only. No tables. No columns. No graphics. Standard section \
            order: Summary, Skills, Experience (reverse chronological), Education, Certifications (if present).

            OUTPUT CONTRACT — return exactly this JSON shape:
            {
              "summary": "string",
              "skills": ["string"],
              "experience": [{"company":"string","title":"string","duration":"string","bullets":["string"]}],
              "education": [{"institution":"string","degree":"string","year":"string"}],
              "certifications": ["string"],
              "unmatchedKeywords": ["string"]
            }
            """;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIClient(@Value("${openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * F6: Generate an ATS-tailored resume JSON string from the candidate's data and target JD.
     *
     * @param candidateSkills     Verified skills from the uploaded resume
     * @param candidateExperience JSON string of experience array from parsed resume
     * @param jobTitle            The target job title
     * @param jobDescription      Full JD text (truncated to 3000 chars by caller)
     * @param requiredSkills      Skills extracted from the JD
     * @param matchingSkills      Skills the candidate already has (from F4)
     * @param missingSkills       Skills the candidate lacks (from F4)
     * @return Raw JSON string conforming to the OUTPUT CONTRACT above
     */
    public String tailorResume(
            List<String> candidateSkills,
            String candidateExperience,
            String jobTitle,
            String jobDescription,
            List<String> requiredSkills,
            List<String> matchingSkills,
            List<String> missingSkills) {

        Map<String, Object> userContent = Map.of(
                "candidateSkills", candidateSkills != null ? candidateSkills : List.of(),
                "candidateExperience", candidateExperience != null ? candidateExperience : "[]",
                "jobTitle", jobTitle != null ? jobTitle : "",
                "jobDescription", jobDescription != null
                        ? jobDescription.substring(0, Math.min(3000, jobDescription.length())) : "",
                "requiredSkills", requiredSkills != null ? requiredSkills : List.of(),
                "matchingSkills", matchingSkills != null ? matchingSkills : List.of(),
                "missingSkills", missingSkills != null ? missingSkills : List.of()
        );

        String userMessage;
        try {
            userMessage = objectMapper.writeValueAsString(userContent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tailoring request", e);
        }

        return callChatCompletion(
                TAILORING_SYSTEM_PROMPT,
                userMessage,
                0.2,   // temperature: per F6 decision log
                2000   // max_tokens: per F6 decision log
        );
    }

    /**
     * F2: Parse a raw text resume into structured JSON.
     */
    public String parseResume(String rawText) {
        return callChatCompletion(
                PARSE_SYSTEM_PROMPT,
                rawText.substring(0, Math.min(10000, rawText.length())),
                0.1,
                1500
        );
    }

    /**
     * F4: Score a resume against a job description.
     */
    public String matchJob(String parsedResume, String jobDescription) {
        try {
            Map<String, String> userContent = Map.of(
                    "resume", parsedResume != null ? parsedResume : "",
                    "jobDescription", jobDescription != null ? jobDescription : ""
            );
            String userMessage = objectMapper.writeValueAsString(userContent);
            return callChatCompletion(
                    MATCH_SYSTEM_PROMPT,
                    userMessage,
                    0.2,
                    1000
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize match request", e);
        }
    }

    /**
     * Shared internal method for all chat completion calls.
     * Enforces json_object response format.
     */
    private String callChatCompletion(String systemPrompt, String userMessage,
                                       double temperature, int maxTokens) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "response_format", Map.of("type", "json_object"),
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_CHAT_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(90))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API returned HTTP " + response.statusCode());
            }

            // Extract content from choices[0].message.content
            Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
            List<?> choices = (List<?>) responseMap.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return (String) message.get("content");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI call failed: " + e.getMessage(), e);
        }
    }
}
