package com.careerpilot.presentation.application.dto;

import com.careerpilot.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for PATCH /api/v1/applications/{id}/status.
 *
 * The client must echo the version number received from the last GET/board response.
 * Hibernate uses it for optimistic locking — a stale version returns HTTP 409.
 */
@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;
}
