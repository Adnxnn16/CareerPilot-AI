package com.careerpilot.domain.resume;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for resume persistence.
 * Zero Spring/JPA annotations — implemented by infrastructure layer.
 *
 * F6 addition: findGeneratedByUserIdAndJobId for idempotency check.
 */
public interface ResumeRepository {
    Resume save(Resume resume);
    Optional<Resume> findById(UUID id);
    List<Resume> findByUserId(UUID userId);

    /**
     * Idempotency guard for F6: returns any existing GENERATED resume for
     * this (user, job) pair that has already reached DONE status.
     * If present, the tailoring endpoint returns this instead of creating a new job.
     */
    Optional<Resume> findGeneratedByUserIdAndJobId(UUID userId, UUID jobId);

    /**
     * Returns the most recent DONE uploaded resume for a user.
     * Used by ResumeTailoringService to auto-select the source resume
     * when no explicit sourceResumeId is provided.
     */
    Optional<Resume> findLatestDoneByUserId(UUID userId);
}
