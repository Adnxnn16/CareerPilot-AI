package com.careerpilot.presentation.resume;

import com.careerpilot.application.resume.ResumeTailoringService;
import com.careerpilot.application.resume.ResumeUploadService;
import com.careerpilot.application.auth.UnauthorizedException;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.domain.user.UserProfile;
import com.careerpilot.domain.user.UserRepository;
import com.careerpilot.infrastructure.cache.CacheService;
import com.careerpilot.presentation.auth.UserController.UserProfileDTO;
import com.careerpilot.infrastructure.storage.R2StorageClient;
import com.careerpilot.presentation.config.RateLimitFilter;
import com.careerpilot.presentation.resume.dto.ResumeDTO;
import com.careerpilot.presentation.resume.dto.TailoringRequestDTO;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * presentation/resume/ResumeController.java — NEW (F6)
 *
 * Exposes:
 *   GET  /api/v1/resumes/{id}                     — polling endpoint (reused from F2)
 *   GET  /api/v1/users/me/resumes                 — list all resumes for current user
 *   POST /api/v1/jobs/{jobId}/resume/tailor        — F6: initiate async tailoring (202)
 *   GET  /api/v1/resumes/{id}/download/pdf         — F6: presigned redirect to PDF
 *   GET  /api/v1/resumes/{id}/download/docx        — F6: presigned redirect to DOCX
 *
 * Rate limits (Bucket4j, in-memory per user):
 *   POST /tailor: 5 / hour / user (per F6 decision log)
 *   All other resume endpoints: covered by RateLimitFilter's 100/min/IP limit
 *
 * Controllers NEVER return JPA entities — only DTOs.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final ResumeTailoringService tailoringService;
    private final ResumeUploadService uploadService;
    private final R2StorageClient r2StorageClient;
    private final UserRepository userRepository;
    private final CacheService cacheService;

    // Per-user rate limiter buckets for tailoring (5/hr/user)
    private final ConcurrentHashMap<UUID, Bucket> tailoringBuckets = new ConcurrentHashMap<>();

    // ── GET /api/v1/resumes/{id} ─────────────────────────────────────────────

    @GetMapping("/api/v1/resumes/{id}")
    public ResponseEntity<ResumeDTO> getResume(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (!userId.equals(resume.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(ResumeDTO.from(resume));
    }

    // ── POST /api/v1/resumes/upload ──────────────────────────────────────────

    @PostMapping("/api/v1/resumes/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        UUID userId = getAuthenticatedUserId();

        try {
            UUID resumeId = uploadService.initiateUpload(userId, file.getOriginalFilename(), file.getBytes());
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "id", resumeId,
                            "status", "PENDING",
                            "message", "Resume upload started. Poll GET /api/v1/resumes/" + resumeId
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to upload resume", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // ── GET /api/v1/users/me/resumes ─────────────────────────────────────────

    @GetMapping("/api/v1/users/me/resumes")
    public ResponseEntity<List<ResumeDTO>> listResumes() {
        UUID userId = getAuthenticatedUserId();
        List<ResumeDTO> dtos = resumeRepository.findByUserId(userId).stream()
                .map(ResumeDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ── POST /api/v1/jobs/{jobId}/resume/tailor — F6 ─────────────────────────

    @PostMapping("/api/v1/jobs/{jobId}/resume/tailor")
    public ResponseEntity<?> tailorResume(
            @PathVariable UUID jobId,
            @Valid @RequestBody TailoringRequestDTO request) {

        UUID userId = getAuthenticatedUserId();

        // Bucket4j rate limit: 5 tailoring requests per hour per user
        Bucket bucket = tailoringBuckets.computeIfAbsent(userId, uid ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
                        .build());

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Rate limit exceeded",
                            "message", "You can generate at most 5 tailored resumes per hour",
                            "status", 429
                    ));
        }

        UUID generatedResumeId = tailoringService.inititateTailoring(
                userId, jobId, request.getSourceResumeId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "id", generatedResumeId,
                        "status", "PENDING",
                        "message", "Resume tailoring started. Poll GET /api/v1/resumes/" + generatedResumeId
                ));
    }

    // ── GET /api/v1/resumes/{id}/download/pdf — F6 ───────────────────────────

    @GetMapping("/api/v1/resumes/{id}/download/pdf")
    public ResponseEntity<Void> downloadPdf(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        Resume resume = getOwnedGeneratedResume(id, userId);

        if (resume.getFileKeyPdf() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String presignedUrl = r2StorageClient.generatePresignedUrl(resume.getFileKeyPdf());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    // ── GET /api/v1/resumes/{id}/download/docx — F6 ──────────────────────────

    @GetMapping("/api/v1/resumes/{id}/download/docx")
    public ResponseEntity<Void> downloadDocx(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        Resume resume = getOwnedGeneratedResume(id, userId);

        if (resume.getFileKeyDocx() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String presignedUrl = r2StorageClient.generatePresignedUrl(resume.getFileKeyDocx());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    // ── POST /api/v1/resumes/{id}/confirm-skills ─────────────────────────────

    @PostMapping("/api/v1/resumes/{id}/confirm-skills")
    public ResponseEntity<UserProfileDTO> confirmSkills(
            @PathVariable UUID id,
            @RequestBody List<String> confirmedSkills) {

        UUID userId = getAuthenticatedUserId();
        Resume resume = getOwnedGeneratedResume(id, userId);

        UserProfile profile = userRepository.findProfileByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        profile.setSkills(confirmedSkills);
        UserProfile updated = userRepository.saveProfile(profile);

        // Evict stale match caches — skills changed, so all cached scores are invalid
        cacheService.evictAllMatchesForUser(userId);

        return ResponseEntity.ok(new UserProfileDTO(updated));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Resume getOwnedGeneratedResume(UUID id, UUID userId) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (!userId.equals(resume.getUserId())) {
            throw new SecurityException("Access denied");
        }
        return resume;
    }

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UUID userId)) {
            throw new UnauthorizedException("Invalid authenticated principal");
        }
        return userId;
    }
}
