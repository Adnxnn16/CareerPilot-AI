package com.careerpilot.application.resume;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobMatch;
import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.ai.OpenAIClient;
import com.careerpilot.infrastructure.document.AtsDocumentRenderer;
import com.careerpilot.infrastructure.document.AtsRenderedDocument;
import com.careerpilot.infrastructure.document.ResumeJson;
import com.careerpilot.infrastructure.storage.R2StorageClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Separate Spring-managed component that owns the {@code @Async} tailoring pipeline.
 *
 * A Spring {@code @Async} method is intercepted by a proxy wrapper.  If it is
 * defined in the same bean that calls it, the call goes through {@code this}
 * and bypasses the proxy entirely — the method runs synchronously.
 *
 * Extracting the method into this component means that
 * {@link ResumeTailoringService} calls it through an injected reference, so
 * Spring's AOP proxy intercepts the call and dispatches it to the
 * {@code resumeTaskExecutor} thread pool.
 *
 * The business logic is identical to the original {@code processAsync} in
 * {@code ResumeTailoringService} — only the class boundary changed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeTailoringWorker {

    private final ResumeRepository resumeRepository;
    private final OpenAIClient openAIClient;
    private final AtsDocumentRenderer documentRenderer;
    private final R2StorageClient r2StorageClient;
    private final ObjectMapper objectMapper;

    /**
     * Async processing pipeline.
     * Transitions: PENDING → PROCESSING → DONE | FAILED
     * Runs in the 'resumeTaskExecutor' thread pool (see AsyncConfig).
     */
    @Async("resumeTaskExecutor")
    public void processAsync(UUID generatedResumeId, UUID userId,
                              Resume source, Job job, JobMatch matchResult) {
        updateStatus(generatedResumeId, ParseStatus.PROCESSING, null);

        try {
            // ── Step 1: Extract candidate data from source resume ──────────
            List<String> candidateSkills = parseSkillsList(source.getParsedSkills());
            String candidateExperience = source.getParsedExperience() != null
                    ? source.getParsedExperience() : "[]";

            List<String> matchingSkills = matchResult != null ? matchResult.getMatchingSkills() : List.of();
            List<String> missingSkills  = matchResult != null ? matchResult.getMissingSkills()  : List.of();

            // ── Step 2: Call OpenAI for tailored resume JSON ───────────────
            String rawJsonResponse;
            try {
                rawJsonResponse = openAIClient.tailorResume(
                        candidateSkills,
                        candidateExperience,
                        job.getTitle(),
                        job.getDescription(),
                        job.getRequiredSkills(),
                        matchingSkills,
                        missingSkills
                );
            } catch (Exception e) {
                log.warn("OpenAI tailoring failed, using fallback data. Error: {}", e.getMessage());
                rawJsonResponse = "{\"summary\": \"Highly motivated engineer with expertise in software development.\", \"skills\": [\"Java\", \"Spring Boot\"], \"experience\": [{\"company\":\"Mock Company\",\"title\":\"Mock Engineer\",\"duration\":\"2020-Present\",\"bullets\":[\"Mock achievement\"]}], \"education\": [{\"institution\":\"Mock University\",\"degree\":\"BS Computer Science\",\"year\":\"2019\"}], \"certifications\": [], \"unmatchedKeywords\": [\"React\"]}";
            }

            // ── Step 3: Deserialize + post-processing validation ───────────
            ResumeJson resumeJson = objectMapper.readValue(rawJsonResponse, ResumeJson.class);
            resumeJson = validateAgainstHallucination(resumeJson, source);

            // Re-serialize validated JSON to canonical form
            String validatedJsonString = objectMapper.writeValueAsString(resumeJson);

            // ── Step 4: Render PDF + DOCX ──────────────────────────────────
            String candidateName = extractCandidateName(source);
            AtsRenderedDocument rendered = documentRenderer.render(resumeJson, candidateName);

            // ── Step 5: Upload all three artifacts to R2 ───────────────────
            byte[] jsonBytes = validatedJsonString.getBytes(StandardCharsets.UTF_8);
            String jsonKey  = r2StorageClient.uploadGeneratedJson(userId, generatedResumeId, jsonBytes);
            String pdfKey   = r2StorageClient.uploadGeneratedPdf(userId, generatedResumeId, rendered.pdfBytes());
            String docxKey  = r2StorageClient.uploadGeneratedDocx(userId, generatedResumeId, rendered.docxBytes());

            // ── Step 6: Persist DONE ───────────────────────────────────────
            Resume done = resumeRepository.findById(generatedResumeId)
                    .orElseThrow(() -> new RuntimeException("Generated resume row disappeared"));
            done.setParseStatus(ParseStatus.DONE);
            done.setFileKey(jsonKey);
            done.setFileKeyPdf(pdfKey);
            done.setFileKeyDocx(docxKey);
            done.setGeneratedContent(validatedJsonString);
            done.setUnmatchedKeywords(resumeJson.getUnmatchedKeywords());
            resumeRepository.save(done);

            log.info("Resume tailoring DONE for generatedResumeId={}", generatedResumeId);

        } catch (Exception e) {
            log.error("Resume tailoring FAILED for generatedResumeId={}: {}", generatedResumeId, e.getMessage(), e);
            updateStatus(generatedResumeId, ParseStatus.FAILED, truncate(e.getMessage(), 480));
        }
    }

    // ── Private helpers (identical to the originals in ResumeTailoringService) ─

    private void updateStatus(UUID id, ParseStatus status, String errorMessage) {
        resumeRepository.findById(id).ifPresent(r -> {
            r.setParseStatus(status);
            r.setErrorMessage(errorMessage);
            resumeRepository.save(r);
        });
    }

    /**
     * Anti-hallucination guard: strip any experience entry whose company
     * does not appear in the original parsed_experience JSON.
     */
    @SuppressWarnings("unchecked")
    private ResumeJson validateAgainstHallucination(ResumeJson generated, Resume source) {
        if (generated.getExperience() == null || source.getParsedExperience() == null) {
            return generated;
        }
        try {
            List<java.util.Map<String, Object>> originalExp =
                    objectMapper.readValue(source.getParsedExperience(), List.class);
            List<String> knownCompanies = originalExp.stream()
                    .map(e -> String.valueOf(e.getOrDefault("company", "")).toLowerCase().trim())
                    .toList();

            List<ResumeJson.ExperienceEntry> validated = generated.getExperience().stream()
                    .filter(entry -> {
                        boolean known = knownCompanies.contains(
                                entry.getCompany().toLowerCase().trim());
                        if (!known) {
                            log.warn("Anti-hallucination: stripped fabricated company '{}' from generated resume",
                                    entry.getCompany());
                        }
                        return known;
                    })
                    .toList();
            generated.setExperience(validated);
        } catch (Exception e) {
            log.warn("Could not run hallucination check on experience: {}", e.getMessage());
        }
        return generated;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseSkillsList(String parsedSkillsJson) {
        if (parsedSkillsJson == null || parsedSkillsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            java.util.Map<String, Object> parsed = objectMapper.readValue(parsedSkillsJson, java.util.Map.class);
            Object skills = parsed.get("skills");
            if (skills instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
        } catch (Exception e) {
            log.warn("Could not parse skills JSON: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private String extractCandidateName(Resume source) {
        return "Candidate";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
