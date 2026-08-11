package com.careerpilot.presentation.application;

import com.careerpilot.application.application.ApplicationService;
import com.careerpilot.application.application.ApplicationService.DuplicateApplicationException;
import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.presentation.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * F5: Application Tracking Board controller.
 *
 * Endpoints:
 *   GET    /api/v1/applications                      — paginated list (optional status filter)
 *   GET    /api/v1/applications/board                — full Kanban board (all columns, one query)
 *   GET    /api/v1/applications/{id}                 — single application
 *   POST   /api/v1/jobs/{jobId}/applications         — create (integrates with job detail page)
 *   PATCH  /api/v1/applications/{id}/status          — Kanban drag (optimistic lock)
 *   PATCH  /api/v1/applications/{id}/notes           — inline notes edit
 *   DELETE /api/v1/applications/{id}                 — hard delete
 *
 * Ownership is enforced in ApplicationService.
 * userId is extracted from SecurityContextHolder, matching the ResumeController pattern.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── GET /api/v1/applications ──────────────────────────────────────────────

    @GetMapping("/api/v1/applications")
    public ResponseEntity<Page<ApplicationDTO>> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        UUID userId = getAuthenticatedUserId();
        ApplicationStatus statusFilter = status != null
                ? ApplicationStatus.valueOf(status.toUpperCase())
                : null;

        Page<ApplicationDTO> result = applicationService.listByUser(userId, statusFilter, page, size, sort, dir);
        return ResponseEntity.ok(result);
    }

    // ── GET /api/v1/applications/board ────────────────────────────────────────

    @GetMapping("/api/v1/applications/board")
    public ResponseEntity<BoardDTO> getBoard() {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(applicationService.getBoard(userId));
    }

    // ── GET /api/v1/applications/{id} ─────────────────────────────────────────

    @GetMapping("/api/v1/applications/{id}")
    public ResponseEntity<ApplicationDTO> getApplication(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        // For GET: return 404 for both not-found AND forbidden (IDOR enumeration resistance)
        return applicationService.listByUser(userId, null, 0, 1, "createdAt", "desc")
                .stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                // Fallback: use the service findById path that enforces ownership via 404
                .orElseGet(() -> {
                    try {
                        // Re-use the board entity approach for single fetch with ownership
                        ApplicationDTO dto = applicationService.getBoard(userId)
                                .getColumns().values().stream()
                                .flatMap(java.util.List::stream)
                                .filter(a -> a.getId().equals(id))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
                        return ResponseEntity.ok(dto);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.notFound().build();
                    }
                });
    }

    // ── POST /api/v1/jobs/{jobId}/applications ────────────────────────────────

    @PostMapping("/api/v1/jobs/{jobId}/applications")
    public ResponseEntity<?> createApplication(
            @PathVariable UUID jobId,
            @Valid @RequestBody(required = false) CreateApplicationRequest request) {

        UUID userId = getAuthenticatedUserId();
        if (request == null) request = new CreateApplicationRequest();

        try {
            ApplicationDTO created = applicationService.create(
                    userId, jobId,
                    request.getResumeId(),
                    request.getNotes(),
                    request.getAppliedDate());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (DuplicateApplicationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Conflict",
                    "message", ex.getMessage(),
                    "existing", ex.getExisting()
            ));
        }
    }

    // ── PATCH /api/v1/applications/{id}/status ────────────────────────────────

    @PatchMapping("/api/v1/applications/{id}/status")
    public ResponseEntity<ApplicationDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {

        UUID userId = getAuthenticatedUserId();
        try {
            ApplicationDTO updated = applicationService.updateStatus(
                    userId, id, request.getStatus(), request.getVersion());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── PATCH /api/v1/applications/{id}/notes ────────────────────────────────

    @PatchMapping("/api/v1/applications/{id}/notes")
    public ResponseEntity<ApplicationDTO> updateNotes(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotesRequest request) {

        UUID userId = getAuthenticatedUserId();
        try {
            ApplicationDTO updated = applicationService.updateNotes(userId, id, request.getNotes());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── DELETE /api/v1/applications/{id} ─────────────────────────────────────

    @DeleteMapping("/api/v1/applications/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        try {
            applicationService.delete(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }
}
