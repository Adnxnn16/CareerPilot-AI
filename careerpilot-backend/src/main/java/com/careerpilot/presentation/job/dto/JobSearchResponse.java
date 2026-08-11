package com.careerpilot.presentation.job.dto;

import com.careerpilot.domain.job.Job;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class JobSearchResponse {
    private UUID id;
    private String externalId;
    private String title;
    private String company;
    private String location;
    private String summary;
    private String jobUrl;
    private List<String> requiredSkills;
    private String source;

    public static JobSearchResponse from(Job job) {
        JobSearchResponse response = new JobSearchResponse();
        response.setId(job.getId());
        response.setExternalId(job.getExternalId());
        response.setTitle(job.getTitle());
        response.setCompany(job.getCompany());
        response.setLocation(job.getLocation());
        response.setSummary(job.getDescription());
        response.setJobUrl(job.getJobUrl());
        response.setRequiredSkills(job.getRequiredSkills() != null ? job.getRequiredSkills() : List.of());
        response.setSource(job.getSource());
        return response;
    }
}
