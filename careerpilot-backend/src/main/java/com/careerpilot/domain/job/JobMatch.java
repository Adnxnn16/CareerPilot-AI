package com.careerpilot.domain.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatch {
    private UUID id;
    private UUID userId;
    private UUID jobId;
    private BigDecimal matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String reasoning;
    private LocalDateTime createdAt;
}
