package com.careerpilot.application.job;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.infrastructure.job.AdzunaClient;
import com.careerpilot.presentation.job.dto.JobSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchService {

    private final AdzunaClient adzunaClient;
    private final JobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<JobSearchResponse> searchJobs(String query, String location, int page, int size) {
        String cacheKey = String.format("jobs:%s:%s:%d:%d", query, location, page, size);
        
        // 1. Check Redis Cache
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj instanceof List) {
            log.info("Cache hit for jobs search: {}", cacheKey);
            return (List<JobSearchResponse>) cachedObj;
        }

        // 2. Fetch from Adzuna (try-catch for fallback)
        List<JobSearchResponse> jobs = new ArrayList<>();
        try {
            log.info("Cache miss, querying Adzuna for: {}", cacheKey);
            // Adzuna API uses 1-based pagination, Spring Data uses 0-based.
            jobs = adzunaClient.searchJobs(query, location, page + 1, size);
        } catch (Exception e) {
            log.warn("Adzuna search failed, falling back to database search", e);
            List<Job> dbJobs = jobRepository.search(query, location, page, size);
            return dbJobs.stream().map(JobSearchResponse::from).collect(Collectors.toList());
        }

        // 3. Save to Job table so they have internal UUIDs
        List<JobSearchResponse> savedJobs = jobs.stream().map(jobDto -> {
            // Find existing by external ID
            Job existing = jobRepository.findByExternalId(jobDto.getExternalId()).orElse(null);
            if (existing != null) {
                return JobSearchResponse.from(existing);
            }

            Job job = Job.builder()
                    .externalId(jobDto.getExternalId())
                    .title(jobDto.getTitle())
                    .company(jobDto.getCompany())
                    .description(jobDto.getSummary())
                    .location(jobDto.getLocation())
                    .jobUrl(jobDto.getJobUrl())
                    .requiredSkills(jobDto.getRequiredSkills() != null ? jobDto.getRequiredSkills() : new ArrayList<>())
                    .source(jobDto.getSource())
                    .build();
            jobRepository.save(job);
            return JobSearchResponse.from(job);
        }).collect(Collectors.toList());

        // 4. Cache in Redis
        redisTemplate.opsForValue().set(cacheKey, savedJobs, Duration.ofHours(1));

        return savedJobs;
    }
}
