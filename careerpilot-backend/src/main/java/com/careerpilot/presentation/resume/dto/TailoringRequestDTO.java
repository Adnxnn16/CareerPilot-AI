package com.careerpilot.presentation.resume.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for POST /jobs/{jobId}/resume/tailor
 */
@Data
public class TailoringRequestDTO {

    @NotNull(message = "sourceResumeId is required")
    private UUID sourceResumeId;
}
