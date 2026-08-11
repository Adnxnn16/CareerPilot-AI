package com.careerpilot.presentation.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PATCH /api/v1/applications/{id}/notes.
 * Notes are nullable (null clears the field; empty string is also valid).
 */
@Data
public class UpdateNotesRequest {

    @Size(max = 4000, message = "Notes must not exceed 4000 characters")
    private String notes;
}
