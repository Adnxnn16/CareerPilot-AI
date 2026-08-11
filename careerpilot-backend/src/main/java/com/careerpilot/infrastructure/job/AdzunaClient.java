package com.careerpilot.infrastructure.job;

import com.careerpilot.presentation.job.dto.JobSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AdzunaClient {

    private final String appId;
    private final String appKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AdzunaClient(
            @Value("${adzuna.app-id}") String appId,
            @Value("${adzuna.app-key}") String appKey) {
        this.appId = appId;
        this.appKey = appKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<JobSearchResponse> searchJobs(String query, String location, int page, int size) {
        String url = UriComponentsBuilder.fromUriString("https://api.adzuna.com/v1/api/jobs/us/search/" + page)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", size)
                .queryParam("what", query)
                .queryParam("where", location)
                .queryParam("content-type", "application/json")
                .build().toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode results = rootNode.get("results");

            List<JobSearchResponse> jobs = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    JobSearchResponse job = new JobSearchResponse();
                    job.setExternalId(node.has("id") ? node.get("id").asText() : null);
                    job.setTitle(node.has("title") ? node.get("title").asText() : null);
                    job.setCompany(node.has("company") && node.get("company").has("display_name") 
                            ? node.get("company").get("display_name").asText() : null);
                    job.setLocation(node.has("location") && node.get("location").has("display_name") 
                            ? node.get("location").get("display_name").asText() : null);
                    job.setSummary(node.has("description") ? node.get("description").asText() : null);
                    job.setJobUrl(node.has("redirect_url") ? node.get("redirect_url").asText() : null);
                    
                    List<String> requiredSkills = new ArrayList<>();
                    if (node.has("category") && node.get("category").has("label")) {
                        requiredSkills.add(node.get("category").get("label").asText());
                    }
                    job.setRequiredSkills(requiredSkills);
                    job.setSource("Adzuna");
                    
                    jobs.add(job);
                }
            }
            return jobs;
        } catch (Exception e) {
            log.error("Failed to search jobs via Adzuna", e);
            throw new RuntimeException("External job search failed", e);
        }
    }
}
