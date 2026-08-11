package com.careerpilot.presentation.auth;

import com.careerpilot.application.auth.UnauthorizedException;
import com.careerpilot.domain.user.User;
import com.careerpilot.domain.user.UserProfile;
import com.careerpilot.domain.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<UserDTO> getCurrentUser() {
        UUID userId = getAuthenticatedUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserProfile profile = userRepository.findProfileByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        return ResponseEntity.ok(new UserDTO(user, profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(@RequestBody UserProfileDTO request) {
        UUID userId = getAuthenticatedUserId();
        UserProfile profile = userRepository.findProfileByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        profile.setLocation(request.getLocation());
        profile.setSkills(request.getSkills());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());

        UserProfile updated = userRepository.saveProfile(profile);
        return ResponseEntity.ok(new UserProfileDTO(updated));
    }

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UUID userId)) {
            throw new UnauthorizedException("Invalid authenticated principal");
        }
        return userId;
    }

    @Data
    public static class UserDTO {
        private UUID id;
        private String email;
        private String name;
        private String role;
        private UserProfileDTO profile;

        public UserDTO(User user, UserProfile profile) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.name = user.getName();
            this.role = user.getRole();
            this.profile = new UserProfileDTO(profile);
        }
    }

    @Data
    public static class UserProfileDTO {
        private String location;
        private List<String> skills;
        private Integer experienceYears;
        private String linkedinUrl;
        private String githubUrl;

        public UserProfileDTO() {}

        public UserProfileDTO(UserProfile profile) {
            this.location = profile.getLocation();
            this.skills = profile.getSkills();
            this.experienceYears = profile.getExperienceYears();
            this.linkedinUrl = profile.getLinkedinUrl();
            this.githubUrl = profile.getGithubUrl();
        }
    }
}
