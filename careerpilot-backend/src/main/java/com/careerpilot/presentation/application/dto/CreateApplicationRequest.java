package com.careerpilot.presentation.application.dto;

import com.careerpilot.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for POST /api/v1/jobs/{jobId}/applications.
 * jobId comes from the path variable; this body carries optional metadata.
 */
@Data
public class CreateApplicationRequest {

    /** FK to a tailored/generated resume to associate with this application. Optional. */
    private UUID resumeId;

    @Size(max = 4000, message = "Notes must not exceed 4000 characters")
    private String notes;

    private LocalDate appliedDate;
}
