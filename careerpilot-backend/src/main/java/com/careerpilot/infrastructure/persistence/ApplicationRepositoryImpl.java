package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.application.Application;
import com.careerpilot.domain.application.ApplicationRepository;
import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.infrastructure.persistence.entity.ApplicationEntity;
import com.careerpilot.infrastructure.persistence.entity.JobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ApplicationRepository using Spring Data JPA.
 * MODIFIED for F5: added optimistic locking propagation,
 * findAllByUserId and findFirstByUserIdAndJobIdOrderByCreatedAtDesc.
 */
@Repository
@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepository {

    private final JpaApplicationRepository repository;

    @Override
    public Application save(Application application) {
        ApplicationEntity entity = toEntity(application);
        ApplicationEntity saved = repository.save(entity);
        // Sync back generated/managed fields
        application.setId(saved.getId());
        application.setCreatedAt(saved.getCreatedAt());
        application.setUpdatedAt(saved.getUpdatedAt());
        application.setVersion(saved.getVersion());
        return application;
    }

    @Override
    public Optional<Application> findById(UUID id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Page<Application> findByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(this::mapToDomain);
    }

    @Override
    public Page<Application> findByUserIdAndStatus(UUID userId, ApplicationStatus status, Pageable pageable) {
        return repository.findByUserIdAndStatus(userId, status, pageable).map(this::mapToDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Application> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdWithJob(userId)
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Application> findFirstByUserIdAndJobIdOrderByCreatedAtDesc(UUID userId, UUID jobId) {
        return repository.findFirstByUserIdAndJobIdOrderByCreatedAtDesc(userId, jobId).map(this::mapToDomain);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ApplicationEntity toEntity(Application a) {
        return ApplicationEntity.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .jobId(a.getJobId())
                .resumeId(a.getResumeId())
                .status(a.getStatus())
                .appliedDate(a.getAppliedDate())
                .notes(a.getNotes())
                .statusChangedAt(a.getStatusChangedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .version(a.getVersion())
                .build();
    }

    private Application mapToDomain(ApplicationEntity e) {
        return Application.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .jobId(e.getJobId())
                .resumeId(e.getResumeId())
                .status(e.getStatus())
                .appliedDate(e.getAppliedDate())
                .notes(e.getNotes())
                .statusChangedAt(e.getStatusChangedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .version(e.getVersion())
                .build();
    }
}
