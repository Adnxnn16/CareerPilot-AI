package com.careerpilot.presentation.job;

import com.careerpilot.application.auth.UnauthorizedException;
import com.careerpilot.application.job.JobMatchingService;
import com.careerpilot.application.job.JobSearchService;
import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.presentation.job.dto.JobSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final JobSearchService jobSearchService;
    private final JobMatchingService jobMatchingService;

    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "1") int page,   // 1-indexed for callers
            @RequestParam(defaultValue = "20") int size) {

        int pageSize = Math.max(1, Math.min(size, 20));   // clamp to [1, 20]
        int zeroBasedPage = Math.max(0, page - 1);        // convert to 0-based for Spring Data PageRequest
        return ResponseEntity.ok(jobSearchService.searchJobs(q, location, zeroBasedPage, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/match")
    public ResponseEntity<?> matchJob(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        try {
            return ResponseEntity.ok(jobMatchingService.matchJob(userId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
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
