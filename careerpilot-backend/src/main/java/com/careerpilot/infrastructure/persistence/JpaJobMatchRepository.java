package com.careerpilot.infrastructure.persistence;

import com.careerpilot.infrastructure.persistence.entity.JobMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaJobMatchRepository extends JpaRepository<JobMatchEntity, UUID> {
    Optional<JobMatchEntity> findByUserIdAndJobId(UUID userId, UUID jobId);
}
