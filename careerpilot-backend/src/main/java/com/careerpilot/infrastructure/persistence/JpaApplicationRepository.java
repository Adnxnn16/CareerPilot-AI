package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.infrastructure.persistence.entity.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ApplicationEntity.
 * MODIFIED for F5: added findAllByUserId (with JOIN FETCH for jobSnapshot)
 * and findFirstByUserIdAndJobIdOrderByCreatedAtDesc (for duplicate detection).
 */
public interface JpaApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    Page<ApplicationEntity> findByUserId(UUID userId, Pageable pageable);

    Page<ApplicationEntity> findByUserIdAndStatus(UUID userId, ApplicationStatus status, Pageable pageable);

    /**
     * Loads all applications for a user in a single query with a LEFT JOIN FETCH on jobs.
     * Used by the Kanban board endpoint — grouped by status in the service layer.
     * Prevents N+1: jobSnapshot fields (title, company, location) are resolved in one DB round-trip.
     */
    @Query("SELECT a FROM ApplicationEntity a LEFT JOIN FETCH a.job j WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<ApplicationEntity> findAllByUserIdWithJob(@Param("userId") UUID userId);

    /**
     * Used for duplicate detection on create.
     * Returns the first application matching (userId, jobId) regardless of status.
     */
    Optional<ApplicationEntity> findFirstByUserIdAndJobIdOrderByCreatedAtDesc(UUID userId, UUID jobId);
}
