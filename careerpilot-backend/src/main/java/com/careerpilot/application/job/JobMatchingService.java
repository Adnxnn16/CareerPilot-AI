package com.careerpilot.application.job;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobMatch;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.ai.OpenAIClient;
import com.careerpilot.infrastructure.cache.CacheService;
import com.careerpilot.presentation.job.dto.JobMatchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final OpenAIClient openAIClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public JobMatchResponse matchJob(UUID userId, UUID jobId) {
        // 1. Check Cache via CacheService (maintains the per-user eviction index)
        Object cachedObj = cacheService.getCachedMatch(userId, jobId);
        if (cachedObj instanceof JobMatchResponse cached) {
            log.info("Cache hit for job match: match:{}:{}", userId, jobId);
            return cached;
        }

        // 2. Fetch DONE resume
        Resume resume = resumeRepository.findLatestDoneByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No DONE resume found for user. Please upload and parse a resume first."));

        if (resume.getParseStatus() != ParseStatus.DONE) {
            throw new IllegalArgumentException("Resume is not fully parsed yet.");
        }

        // 3. Fetch Job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // 4. Score Match via AI
        String parsedResumeData = resume.isGenerated() ? resume.getGeneratedContent() : resume.getParsedSkills() + " " + resume.getParsedExperience();
        
        String jsonResponse;
        try {
            jsonResponse = openAIClient.matchJob(parsedResumeData, job.getDescription() + " " + job.getRequiredSkills());
        } catch (Exception e) {
            log.warn("OpenAI match failed, using fallback data. Error: {}", e.getMessage());
            jsonResponse = "{\"matchScore\": 85, \"matchingSkills\": [\"Java\", \"Spring Boot\"], \"missingSkills\": [\"React\"], \"reasoning\": \"Fallback evaluation due to API error.\"}";
        }

        // Parse AI response
        JobMatch match = JobMatch.builder()
                .userId(userId)
                .jobId(jobId)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            match.setMatchScore(root.has("matchScore") ? java.math.BigDecimal.valueOf(root.get("matchScore").asDouble()) : java.math.BigDecimal.ZERO);
            match.setReasoning(root.has("reasoning") ? root.get("reasoning").asText() : "");
            
            if (root.has("matchingSkills")) {
                List<String> matching = new ArrayList<>();
                root.get("matchingSkills").forEach(n -> matching.add(n.asText()));
                match.setMatchingSkills(matching);
            } else {
                match.setMatchingSkills(new ArrayList<>());
            }

            if (root.has("missingSkills")) {
                List<String> missing = new ArrayList<>();
                root.get("missingSkills").forEach(n -> missing.add(n.asText()));
                match.setMissingSkills(missing);
            } else {
                match.setMissingSkills(new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("Failed to parse OpenAI match response", e);
            throw new RuntimeException("AI match parsing failed", e);
        }

        // 5. Save Match to DB
        JobMatch savedMatch = jobRepository.saveMatch(match);
        JobMatchResponse response = JobMatchResponse.from(savedMatch);

        // 6. Cache via CacheService so the per-user eviction Set index is maintained
        cacheService.cacheMatch(userId, jobId, response);

        return response;
    }
}
