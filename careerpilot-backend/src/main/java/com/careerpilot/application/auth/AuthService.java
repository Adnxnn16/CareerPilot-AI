package com.careerpilot.application.auth;

import com.careerpilot.domain.user.User;
import com.careerpilot.domain.user.UserProfile;
import com.careerpilot.domain.user.UserRepository;
import com.careerpilot.presentation.config.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public User register(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .userId(savedUser.getId())
                .skills(new ArrayList<>())
                .build();
        userRepository.saveProfile(profile);

        return savedUser;
    }

    public Optional<User> authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()));
    }

    /**
     * Generates a fresh access + refresh token pair, stores the SHA-256 hash
     * of the refresh token on the user record, and persists the change.
     * Must be called through a transaction boundary that includes the subsequent save.
     */
    @Transactional
    public TokenPair issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getRole()); // real role from DB
        String refresh = jwtService.generateRefreshToken(user.getId());
        user.setCurrentRefreshTokenHash(sha256Hex(refresh));
        userRepository.save(user);
        return new TokenPair(access, refresh);
    }

    /**
     * Validates the incoming refresh token against the stored hash.
     * On hash mismatch the stored hash is immediately nulled (session killed) before
     * throwing, to guard against token reuse after theft.
     */
    @Transactional
    public TokenPair rotateTokens(String incomingRefreshToken) {
        if (!jwtService.validateToken(incomingRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        UUID userId = UUID.fromString(jwtService.getUserIdFromToken(incomingRefreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid session"));

        String incomingHash = sha256Hex(incomingRefreshToken);
        if (user.getCurrentRefreshTokenHash() == null
                || !user.getCurrentRefreshTokenHash().equals(incomingHash)) {
            // Possible token reuse / theft — kill the session immediately
            user.setCurrentRefreshTokenHash(null);
            userRepository.save(user);
            throw new UnauthorizedException("Refresh token invalid or already used");
        }

        return issueTokens(user); // also persists the new hash
    }

    /**
     * Invalidates the user's current session by clearing the stored refresh token hash.
     */
    @Transactional
    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setCurrentRefreshTokenHash(null);
            userRepository.save(u);
        });
    }

    private String sha256Hex(String token) {
        // commons-codec is a transitive dep of spring-boot-starter-security;
        // add explicitly to pom/gradle only if the build fails to resolve it.
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(token);
    }
}
