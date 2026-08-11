package com.careerpilot.infrastructure.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Java representation of the JSON resume structure returned by OpenAI.
 * Used for deserialization, post-processing validation, and rendering.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeJson {

    private String summary;
    private List<String> skills;
    private List<ExperienceEntry> experience;
    private List<EducationEntry> education;
    private List<String> certifications;
    private List<String> unmatchedKeywords;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceEntry {
        private String company;
        private String title;
        private String duration;
        private List<String> bullets;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationEntry {
        private String institution;
        private String degree;
        private String year;
    }
}
