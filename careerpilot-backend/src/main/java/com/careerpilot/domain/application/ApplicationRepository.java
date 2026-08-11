package com.careerpilot.domain.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for Application.
 * MODIFIED for F5: added findAllByUserId and findFirstByUserIdAndJobIdOrderByCreatedAtDesc.
 */
public interface ApplicationRepository {
    Application save(Application application);
    Optional<Application> findById(UUID id);
    Page<Application> findByUserId(UUID userId, Pageable pageable);
    Page<Application> findByUserIdAndStatus(UUID userId, ApplicationStatus status, Pageable pageable);
    void deleteById(UUID id);

    /**
     * Returns all applications for a user — unbounded.
     * Used for the Kanban board view (grouped by status in the service layer).
     * Bounded by user ownership; realistic max ~200–300 rows.
     */
    List<Application> findAllByUserId(UUID userId);

    /**
     * Used for duplicate detection before creating a new application.
     * Returns the first application for the given (user, job) pair regardless of status.
     */
    Optional<Application> findFirstByUserIdAndJobIdOrderByCreatedAtDesc(UUID userId, UUID jobId);
}
