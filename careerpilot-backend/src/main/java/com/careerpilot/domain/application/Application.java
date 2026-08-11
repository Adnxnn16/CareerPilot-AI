package com.careerpilot.domain.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain object for an application.
 * MODIFIED for F5: added resumeId, statusChangedAt, version (optimistic locking).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    private UUID id;
    private UUID userId;
    private UUID jobId;

    /** FK to a generated/tailored resume (nullable). Added in V3. */
    private UUID resumeId;

    private ApplicationStatus status;
    private LocalDate appliedDate;

    @Builder.Default
    private String notes = "";

    /** Timestamp of the last status change. Set by service layer on every transition. Added in V3. */
    private LocalDateTime statusChangedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Hibernate @Version counter used for optimistic locking.
     * Returned in every response DTO so the client can echo it back on PATCH /status.
     * Added in V3.
     */
    private Long version;
}
