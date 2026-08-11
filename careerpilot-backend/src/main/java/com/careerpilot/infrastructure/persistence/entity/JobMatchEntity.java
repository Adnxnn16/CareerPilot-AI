package com.careerpilot.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID jobId;
    
    private BigDecimal matchScore;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> matchingSkills;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> missingSkills;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
