package com.careerpilot.infrastructure.persistence;

import com.careerpilot.infrastructure.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaJobRepository extends JpaRepository<JobEntity, UUID> {
    Optional<JobEntity> findByExternalId(String externalId);

    @Query("SELECT j FROM JobEntity j WHERE (LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.company) LIKE LOWER(CONCAT('%', :query, '%'))) AND LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    Page<JobEntity> search(@Param("query") String query, @Param("location") String location, Pageable pageable);
}
