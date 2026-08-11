package com.careerpilot.presentation.auth;

import com.careerpilot.application.auth.AuthService;
import com.careerpilot.application.auth.TokenPair;
import com.careerpilot.application.auth.UnauthorizedException;
import com.careerpilot.domain.user.User;
import com.careerpilot.presentation.auth.dto.LoginRequest;
import com.careerpilot.presentation.auth.dto.MessageResponse;
import com.careerpilot.presentation.auth.dto.RegisterRequest;
import com.careerpilot.presentation.config.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService; // retained for validateToken + getUserIdFromToken in logout

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = authService.register(request.getEmail(), request.getPassword(), request.getName());
        setCookies(response, authService.issueTokens(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.authenticate(request.getEmail(), request.getPassword())
                .map(user -> {
                    setCookies(response, authService.issueTokens(user));
                    return ResponseEntity.ok(new MessageResponse("Login successful"));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid credentials")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<MessageResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("No refresh token provided"));
        }
        try {
            setCookies(response, authService.rotateTokens(refreshToken));
            return ResponseEntity.ok(new MessageResponse("Token refreshed"));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            HttpServletResponse response) {
        // Best-effort: revoke server-side session if the token is still valid
        if (accessToken != null && jwtService.validateToken(accessToken)) {
            authService.logout(UUID.fromString(jwtService.getUserIdFromToken(accessToken)));
        }
        addCookie(response, "access_token", "", 0);
        addCookie(response, "refresh_token", "", 0);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    // --- helpers ---

    private void setCookies(HttpServletResponse response, TokenPair tokens) {
        addCookie(response, "access_token", tokens.accessToken(), 900);      // 15 min
        addCookie(response, "refresh_token", tokens.refreshToken(), 604800); // 7 days
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
