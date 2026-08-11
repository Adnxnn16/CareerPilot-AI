package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.user.User;
import com.careerpilot.domain.user.UserProfile;
import com.careerpilot.domain.user.UserRepository;
import com.careerpilot.infrastructure.persistence.entity.UserEntity;
import com.careerpilot.infrastructure.persistence.entity.UserProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository userRepository;
    private final JpaUserProfileRepository profileRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .currentRefreshTokenHash(user.getCurrentRefreshTokenHash())
                .build();
        UserEntity saved = userRepository.save(entity);
        user.setId(saved.getId());
        user.setCreatedAt(saved.getCreatedAt());
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::mapToDomain);
    }

    @Override
    public UserProfile saveProfile(UserProfile profile) {
        UserProfileEntity entity = UserProfileEntity.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .location(profile.getLocation())
                .skills(profile.getSkills())
                .experienceYears(profile.getExperienceYears())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .build();
        UserProfileEntity saved = profileRepository.save(entity);
        profile.setId(saved.getId());
        return profile;
    }

    @Override
    public Optional<UserProfile> findProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId).map(e -> UserProfile.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .location(e.getLocation())
                .skills(e.getSkills())
                .experienceYears(e.getExperienceYears())
                .linkedinUrl(e.getLinkedinUrl())
                .githubUrl(e.getGithubUrl())
                .build());
    }

    private User mapToDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .name(entity.getName())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .currentRefreshTokenHash(entity.getCurrentRefreshTokenHash())
                .build();
    }
}
