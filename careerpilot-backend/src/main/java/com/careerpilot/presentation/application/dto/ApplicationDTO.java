package com.careerpilot.presentation.application.dto;

import com.careerpilot.domain.application.Application;
import com.careerpilot.infrastructure.persistence.entity.ApplicationEntity;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single application.
 *
 * NOTE: userId is intentionally OMITTED to prevent IDOR information leakage.
 * version MUST be included so the client can echo it back on PATCH /status.
 * jobSnapshot is populated from the JOIN-fetched JobEntity (null if job was deleted).
 */
@Value
@Builder
public class ApplicationDTO {

    UUID id;
    UUID jobId;
    UUID resumeId;
    String status;
    LocalDate appliedDate;
    String notes;
    LocalDateTime statusChangedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long version;

    /** Snapshot of job metadata, resolved via JOIN FETCH. Null if the job was deleted. */
    JobSnapshot jobSnapshot;

    @Value
    @Builder
    public static class JobSnapshot {
        String title;
        String company;
        String location;
    }

    /**
     * Maps a domain Application to an ApplicationDTO.
     * jobSnapshot is built from the Application's jobId + names provided by the service layer.
     * For the simple case (no job data pre-loaded), pass null for snapshot fields.
     */
    public static ApplicationDTO from(Application app, String jobTitle, String company, String location) {
        return ApplicationDTO.builder()
                .id(app.getId())
                .jobId(app.getJobId())
                .resumeId(app.getResumeId())
                .status(app.getStatus() != null ? app.getStatus().name() : null)
                .appliedDate(app.getAppliedDate())
                .notes(app.getNotes())
                .statusChangedAt(app.getStatusChangedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .version(app.getVersion())
                .jobSnapshot(
                        (jobTitle != null || company != null)
                                ? JobSnapshot.builder()
                                .title(jobTitle)
                                .company(company)
                                .location(location)
                                .build()
                                : null
                )
                .build();
    }

    /** Overload for when no job snapshot is available (e.g. job was deleted). */
    public static ApplicationDTO from(Application app) {
        return from(app, null, null, null);
    }

    /**
     * Maps directly from a JPA entity (with pre-loaded @ManyToOne job).
     * Used by the board endpoint after JOIN FETCH.
     */
    public static ApplicationDTO fromEntity(ApplicationEntity entity) {
        String title = null, company = null, location = null;
        if (entity.getJob() != null) {
            title = entity.getJob().getTitle();
            company = entity.getJob().getCompany();
            location = entity.getJob().getLocation();
        }
        return ApplicationDTO.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .resumeId(entity.getResumeId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .appliedDate(entity.getAppliedDate())
                .notes(entity.getNotes())
                .statusChangedAt(entity.getStatusChangedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .jobSnapshot(
                        title != null
                                ? JobSnapshot.builder()
                                .title(title)
                                .company(company)
                                .location(location)
                                .build()
                                : null
                )
                .build();
    }
}
