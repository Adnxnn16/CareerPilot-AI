package com.careerpilot.presentation.job.dto;

import com.careerpilot.domain.job.JobMatch;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class JobMatchResponse implements Serializable {
    private BigDecimal matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String reasoning;
    
    // Computed client-side typically, but providing a helper color string here
    private String matchCategory; // RED, AMBER, GREEN

    public static JobMatchResponse from(JobMatch match) {
        JobMatchResponse response = new JobMatchResponse();
        response.setMatchScore(match.getMatchScore());
        response.setMatchingSkills(match.getMatchingSkills() != null ? match.getMatchingSkills() : List.of());
        response.setMissingSkills(match.getMissingSkills() != null ? match.getMissingSkills() : List.of());
        response.setReasoning(match.getReasoning());
        
        double score = match.getMatchScore() != null ? match.getMatchScore().doubleValue() : 0;
        if (score < 40) {
            response.setMatchCategory("RED");
        } else if (score < 70) {
            response.setMatchCategory("AMBER");
        } else {
            response.setMatchCategory("GREEN");
        }
        return response;
    }
}
