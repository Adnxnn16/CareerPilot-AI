package com.careerpilot.application.resume;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import com.careerpilot.application.job.JobMatchingService;

/**
 * application/resume/ResumeTailoringService.java — NEW (F6)
 *
 * Orchestrates the full JD-Matched ATS Resume generation pipeline:
 *   1. Validate source resume (DONE + UPLOADED)
 *   2. Load target job
 *   3. Load F4 match result (automatically scoring if missing)
 *   4. Idempotency check (return existing DONE record if found)
 *   5. Insert PENDING row → return id immediately (caller returns 202)
 *   6. @Async: PROCESSING → OpenAI tailoring → validate output →
 *              render PDF+DOCX → upload to R2 → DONE (or FAILED)
 *
 * Dependencies:
 *   ResumeRepository    — load source resume, persist generated resume
 *   JobRepository       — load job + match record
 *   OpenAIClient        — calls the tailoring prompt (F6-specific method)
 *   AtsDocumentRenderer — converts JSON → PDF + DOCX bytes
 *   R2StorageClient     — uploads all three artifacts to Cloudflare R2
 *   JobMatchingService  — automatically scores job if missing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTailoringService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ResumeTailoringWorker resumeTailoringWorker; // separate bean — proxy fires @Async correctly
    private final JobMatchingService jobMatchingService;

    /**
     * Synchronous entry point — called by the controller.
     * Creates a PENDING record and returns its ID immediately (202).
     *
     * @param userId         Authenticated user's ID
     * @param jobId          The target job
     * @param sourceResumeId The UPLOADED DONE resume to tailor from
     * @return UUID of the new PENDING generated resume row
     */
    public UUID inititateTailoring(UUID userId, UUID jobId, UUID sourceResumeId) {
        // 1. Validate source resume
        Resume source = resumeRepository.findById(sourceResumeId)
                .orElseThrow(() -> new IllegalArgumentException("Source resume not found"));

        if (!userId.equals(source.getUserId())) {
            throw new SecurityException("Source resume does not belong to this user");
        }
        if (source.getParseStatus() != ParseStatus.DONE) {
            throw new IllegalArgumentException("Source resume has not finished processing");
        }
        if (source.isGenerated()) {
            throw new IllegalArgumentException("Source resume must be an uploaded resume, not a generated one");
        }

        // 2. Validate target job exists
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        // 3. Require a job_matches record (F4 must have been run first)
        if (jobRepository.findMatchByUserIdAndJobId(userId, jobId).isEmpty()) {
            log.info("Job match not found for user {} and job {}. Scoring now before tailoring.", userId, jobId);
            try {
                jobMatchingService.matchJob(userId, jobId);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to automatically score this job: " + e.getMessage(), e);
            }
        }

        // 4. Idempotency: return existing DONE or PROCESSING generated resume
        Optional<Resume> existing = resumeRepository.findGeneratedByUserIdAndJobId(userId, jobId);
        if (existing.isPresent()) {
            Resume existingResume = existing.get();
            if (existingResume.getParseStatus() == ParseStatus.DONE ||
                    existingResume.getParseStatus() == ParseStatus.PROCESSING ||
                    existingResume.getParseStatus() == ParseStatus.PENDING) {
                log.info("Returning existing generated resume {} for user {} / job {}",
                        existingResume.getId(), userId, jobId);
                return existingResume.getId();
            }
        }

        // 5. Create PENDING record
        Resume pending = Resume.builder()
                .userId(userId)
                .sourceType("GENERATED")
                .sourceJobId(jobId)
                .parseStatus(ParseStatus.PENDING)
                .build();
        resumeRepository.save(pending);
        
        UUID generatedId = pending.getId();

        // 6. Kick off async processing through the injected worker bean so Spring's
        //    proxy intercepts the call and dispatches it to resumeTaskExecutor.
        resumeTailoringWorker.processAsync(generatedId, userId, source, job,
                jobRepository.findMatchByUserIdAndJobId(userId, jobId).orElse(null));

        return generatedId;
    }
}
