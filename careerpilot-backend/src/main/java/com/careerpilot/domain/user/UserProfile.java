package com.careerpilot.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private UUID id;
    private UUID userId;
    private String location;
    private List<String> skills;
    private Integer experienceYears;
    private String linkedinUrl;
    private String githubUrl;
}
