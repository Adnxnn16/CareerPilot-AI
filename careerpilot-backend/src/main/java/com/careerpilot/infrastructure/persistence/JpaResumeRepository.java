package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.infrastructure.persistence.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaResumeRepository extends JpaRepository<ResumeEntity, UUID> {

    List<ResumeEntity> findByUserId(UUID userId);

    /**
     * F6: idempotency check — return any GENERATED resume (DONE or PROCESSING)
     * for a given (userId, sourceJobId) pair to avoid duplicate generations.
     */
    Optional<ResumeEntity> findByUserIdAndSourceJobIdAndSourceType(
            UUID userId, UUID sourceJobId, String sourceType);

    /**
     * F6: auto-select the most recent DONE uploaded resume for a user
     * when no explicit sourceResumeId is provided.
     */
    @Query("SELECT r FROM ResumeEntity r WHERE r.userId = :userId " +
           "AND r.sourceType = 'UPLOADED' AND r.parseStatus = :status " +
           "ORDER BY r.id DESC LIMIT 1")
    Optional<ResumeEntity> findLatestByUserIdAndParseStatus(
            @Param("userId") UUID userId,
            @Param("status") ParseStatus status);
}
