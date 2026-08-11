package com.careerpilot.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    
    UserProfile saveProfile(UserProfile profile);
    Optional<UserProfile> findProfileByUserId(UUID userId);
}
