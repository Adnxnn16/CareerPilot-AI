package com.careerpilot.domain.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private UUID id;
    private String externalId;
    private String title;
    private String company;
    private String description;
    private List<String> requiredSkills;
    private String location;
    private String jobUrl;
    private String source;
    private LocalDateTime cachedAt;
}
