package com.careerpilot.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID userId;
    
    @Column(nullable = false)
    private String action;
    
    @JdbcTypeCode(SqlTypes.JSON)
    private String details;
    
    private String ipAddress;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
