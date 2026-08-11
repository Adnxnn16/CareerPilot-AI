package com.careerpilot.application.auth;

import com.careerpilot.domain.user.User;
import com.careerpilot.domain.user.UserProfile;
import com.careerpilot.domain.user.UserRepository;
import com.careerpilot.presentation.config.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegister_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        
        User savedUser = new User();
        savedUser.setId(java.util.UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register("test@test.com", "pass", "Test");
        
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
        verify(userRepository).saveProfile(any(UserProfile.class));
    }

    @Test
    void testRegister_DuplicateEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        
        assertThrows(IllegalArgumentException.class, () -> 
            authService.register("test@test.com", "pass", "Test"));
    }

    @Test
    void testAuthenticate_Success() {
        User u = new User();
        u.setPasswordHash("hashed");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Optional<User> result = authService.authenticate("test@test.com", "pass");
        assertTrue(result.isPresent());
    }

    @Test
    void testAuthenticate_Failure() {
        User u = new User();
        u.setPasswordHash("hashed");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Optional<User> result = authService.authenticate("test@test.com", "pass");
        assertFalse(result.isPresent());
    }
}
