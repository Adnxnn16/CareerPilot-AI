package com.careerpilot.application.application;

import com.careerpilot.domain.application.Application;
import com.careerpilot.domain.application.ApplicationRepository;
import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.persistence.JpaApplicationRepository;
import com.careerpilot.infrastructure.persistence.entity.ApplicationEntity;
import com.careerpilot.presentation.application.dto.ApplicationDTO;
import com.careerpilot.presentation.application.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * F5: Application service — business logic for the Kanban tracking board.
 *
 * Responsibilities:
 *  1. create          — duplicate check, insert SAVED application
 *  2. updateStatus    — ownership + transition validation, optimistic lock
 *  3. updateNotes     — ownership check only
 *  4. delete          — ownership check, hard delete
 *  5. listByUser      — paginated list with optional status filter
 *  6. getBoard        — all columns in one DB round-trip, grouped in Java
 *
 * Ownership assertion (IDOR defence): every mutating method fetches the
 * application first and calls assertOwnership() before proceeding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JpaApplicationRepository jpaApplicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    private static final int MAX_PAGE_SIZE = 100;

    // ── 1. Create ─────────────────────────────────────────────────────────────

    @Transactional
    public ApplicationDTO create(UUID userId, UUID jobId,
                                 UUID resumeId, String notes, LocalDate appliedDate) {
        // Verify job exists
        jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Verify resumeId ownership if provided
        if (resumeId != null) {
            resumeRepository.findById(resumeId)
                    .filter(r -> userId.equals(r.getUserId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Resume not found or does not belong to this user"));
        }

        // Duplicate check: only one active (non-REJECTED) application per (user, job)
        applicationRepository.findFirstByUserIdAndJobIdOrderByCreatedAtDesc(userId, jobId)
                .filter(a -> a.getStatus() != ApplicationStatus.REJECTED)
                .ifPresent(existing -> {
                    throw new DuplicateApplicationException(
                            "You already have an active application for this job",
                            ApplicationDTO.from(existing));
                });

        LocalDateTime now = LocalDateTime.now();
        Application app = Application.builder()
                .userId(userId)
                .jobId(jobId)
                .resumeId(resumeId)
                .status(ApplicationStatus.SAVED)
                .notes(notes)
                .appliedDate(appliedDate)
                .statusChangedAt(now)
                .build();

        Application saved = applicationRepository.save(app);
        log.info("Application created: id={} userId={} jobId={}", saved.getId(), userId, jobId);
        return ApplicationDTO.from(saved);
    }

    // ── 2. Update Status ──────────────────────────────────────────────────────

    @Transactional
    public ApplicationDTO updateStatus(UUID userId, UUID applicationId,
                                       ApplicationStatus newStatus, Long clientVersion) {
        Application app = findAndAssertOwnership(applicationId, userId);

        if (!app.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalStateException(String.format(
                    "Status transition from %s to %s is not allowed",
                    app.getStatus(), newStatus));
        }

        // Apply client version for Hibernate optimistic locking
        app.setVersion(clientVersion);
        app.setStatus(newStatus);
        app.setStatusChangedAt(LocalDateTime.now());

        Application saved = applicationRepository.save(app);
        log.info("Application {} status changed: {} → {} by user {}",
                applicationId, app.getStatus(), newStatus, userId);
        return ApplicationDTO.from(saved);
    }

    // ── 3. Update Notes ───────────────────────────────────────────────────────

    @Transactional
    public ApplicationDTO updateNotes(UUID userId, UUID applicationId, String notes) {
        Application app = findAndAssertOwnership(applicationId, userId);
        app.setNotes(notes);
        Application saved = applicationRepository.save(app);
        return ApplicationDTO.from(saved);
    }

    // ── 4. Delete (hard) ──────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        // For GET/DELETE: return 404 for both not-found AND forbidden (enumeration resistance)
        Application app = applicationRepository.findById(applicationId)
                .filter(a -> userId.equals(a.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        applicationRepository.deleteById(applicationId);
        log.info("Application {} deleted by user {}", applicationId, userId);
    }

    // ── 5. List (paginated) ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApplicationDTO> listByUser(UUID userId, ApplicationStatus status,
                                           int page, int size, String sortField, String sortDir) {
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (status != null) {
            return applicationRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(ApplicationDTO::from);
        }
        return applicationRepository.findByUserId(userId, pageable)
                .map(ApplicationDTO::from);
    }

    // ── 6. Board (all columns, one query) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public BoardDTO getBoard(UUID userId) {
        // Single JOIN FETCH query for all of the user's applications + job metadata
        List<ApplicationEntity> entities = jpaApplicationRepository.findAllByUserIdWithJob(userId);

        // Pre-populate all 6 columns as empty lists so the frontend always has all keys
        Map<String, List<ApplicationDTO>> columns = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            columns.put(s.name(), new ArrayList<>());
        }

        // Group into columns in Java — one DB round-trip total
        for (ApplicationEntity entity : entities) {
            String key = entity.getStatus() != null ? entity.getStatus().name() : "SAVED";
            columns.get(key).add(ApplicationDTO.fromEntity(entity));
        }

        return BoardDTO.builder().columns(columns).build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Fetches an application and asserts ownership.
     * For PATCH operations: throws SecurityException (→ 403) on ownership failure.
     */
    private Application findAndAssertOwnership(UUID applicationId, UUID userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!userId.equals(app.getUserId())) {
            throw new SecurityException("Access denied");
        }
        return app;
    }

    // ── DuplicateApplicationException (inner class) ───────────────────────────

    /**
     * Thrown when a user tries to create a second active application for the same job.
     * The controller catches this to return 409 with the existing ApplicationDTO.
     */
    public static class DuplicateApplicationException extends RuntimeException {
        private final ApplicationDTO existing;

        public DuplicateApplicationException(String message, ApplicationDTO existing) {
            super(message);
            this.existing = existing;
        }

        public ApplicationDTO getExisting() {
            return existing;
        }
    }
}
