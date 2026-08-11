package com.careerpilot.infrastructure.persistence.entity;

import com.careerpilot.domain.application.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the applications table.
 * MODIFIED for F5 (V3 migration): added resumeId, statusChangedAt, version,
 * and a read-only lazy @ManyToOne to JobEntity for JOIN FETCH / jobSnapshot.
 */
@Entity
@Table(name = "applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** Writable FK column. Used for inserts/updates. */
    @Column(name = "job_id")
    private UUID jobId;

    /**
     * Read-only lazy join to JobEntity.
     * Used ONLY for JOIN FETCH queries that produce jobSnapshot in ApplicationDTO.
     * insertable=false + updatable=false prevents conflict with the writable jobId column above.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", insertable = false, updatable = false)
    private JobEntity job;

    /** FK to resumes.id — nullable. Links a tailored resume to this application. Added in V3. */
    @Column(name = "resume_id")
    private UUID resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private LocalDate appliedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Timestamp of the last status change. Managed by ApplicationService, not a DB trigger. Added in V3. */
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Hibernate optimistic locking version counter. Added in V3.
     * Incremented automatically by Hibernate on each UPDATE.
     * Throws OptimisticLockException if the client's version is stale.
     */
    @Version
    private Long version;
}
