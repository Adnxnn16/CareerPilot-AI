package com.careerpilot.domain.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface JobRepository {
    Job save(Job job);
    Optional<Job> findById(UUID id);
    Optional<Job> findByExternalId(String externalId);
    
    JobMatch saveMatch(JobMatch match);
    Optional<JobMatch> findMatchByUserIdAndJobId(UUID userId, UUID jobId);
    
    List<Job> search(String query, String location, int page, int size);
}
