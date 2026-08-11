package com.careerpilot.presentation.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for GET /api/v1/applications/board.
 * Returns all applications grouped by status, ready for Kanban column rendering.
 *
 * Example structure:
 * {
 *   "columns": {
 *     "SAVED":     [...],
 *     "APPLIED":   [...],
 *     "SCREENING": [...],
 *     "INTERVIEW": [...],
 *     "OFFER":     [...],
 *     "REJECTED":  [...]
 *   }
 * }
 */
@Value
@Builder
public class BoardDTO {

    /**
     * Map of status name → list of application DTOs in that column.
     * All 6 status keys are always present (empty list if no applications in that column).
     */
    Map<String, List<ApplicationDTO>> columns;
}
