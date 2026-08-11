package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobMatch;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.infrastructure.persistence.entity.JobEntity;
import com.careerpilot.infrastructure.persistence.entity.JobMatchEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepository {
    private final JpaJobRepository jobRepository;
    private final JpaJobMatchRepository matchRepository;

    @Override
    public Job save(Job job) {
        JobEntity entity = JobEntity.builder()
                .id(job.getId())
                .externalId(job.getExternalId())
                .title(job.getTitle())
                .company(job.getCompany())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .location(job.getLocation())
                .jobUrl(job.getJobUrl())
                .source(job.getSource())
                .cachedAt(job.getCachedAt())
                .build();
        JobEntity saved = jobRepository.save(entity);
        job.setId(saved.getId());
        job.setCachedAt(saved.getCachedAt());
        return job;
    }

    @Override
    public Optional<Job> findById(UUID id) {
        return jobRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<Job> findByExternalId(String externalId) {
        return jobRepository.findByExternalId(externalId).map(this::mapToDomain);
    }

    @Override
    public JobMatch saveMatch(JobMatch match) {
        JobMatchEntity entity = JobMatchEntity.builder()
                .id(match.getId())
                .userId(match.getUserId())
                .jobId(match.getJobId())
                .matchScore(match.getMatchScore())
                .matchingSkills(match.getMatchingSkills())
                .missingSkills(match.getMissingSkills())
                .reasoning(match.getReasoning())
                .createdAt(match.getCreatedAt())
                .build();
        JobMatchEntity saved = matchRepository.save(entity);
        match.setId(saved.getId());
        match.setCreatedAt(saved.getCreatedAt());
        return match;
    }

    @Override
    public Optional<JobMatch> findMatchByUserIdAndJobId(UUID userId, UUID jobId) {
        return matchRepository.findByUserIdAndJobId(userId, jobId).map(e -> JobMatch.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .jobId(e.getJobId())
                .matchScore(e.getMatchScore())
                .matchingSkills(e.getMatchingSkills())
                .missingSkills(e.getMissingSkills())
                .reasoning(e.getReasoning())
                .createdAt(e.getCreatedAt())
                .build());
    }

    @Override
    public List<Job> search(String query, String location, int page, int size) {
        return jobRepository.search(query, location, PageRequest.of(page, size))
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    private Job mapToDomain(JobEntity entity) {
        return Job.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .company(entity.getCompany())
                .description(entity.getDescription())
                .requiredSkills(entity.getRequiredSkills())
                .location(entity.getLocation())
                .jobUrl(entity.getJobUrl())
                .source(entity.getSource())
                .cachedAt(entity.getCachedAt())
                .build();
    }
}
