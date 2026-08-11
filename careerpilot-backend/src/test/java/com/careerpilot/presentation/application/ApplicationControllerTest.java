package com.careerpilot.presentation.application;

import com.careerpilot.application.application.ApplicationService;
import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.presentation.application.dto.ApplicationDTO;
import com.careerpilot.presentation.application.dto.BoardDTO;
import com.careerpilot.presentation.application.dto.CreateApplicationRequest;
import com.careerpilot.presentation.application.dto.UpdateStatusRequest;
import com.careerpilot.presentation.config.SecurityConfig;
import com.careerpilot.presentation.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;
    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    private UUID userId;
    private UUID jobId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        
        // Manually set SecurityContext to use UUID principal, avoiding ClassCastException from @WithMockUser
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            Mockito.doAnswer(invocation -> {
                jakarta.servlet.ServletRequest req = invocation.getArgument(0);
                jakarta.servlet.ServletResponse res = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(req, res);
                return null;
            }).when(jwtAuthFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createApplication_unauthenticated_401() throws Exception {
        SecurityContextHolder.clearContext();
        try {
            Mockito.doAnswer(invocation -> {
                HttpServletResponse res = invocation.getArgument(1);
                res.sendError(401, "Unauthorized");
                return null;
            }).when(jwtAuthFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized()); // Or 403 depending on Spring Security setup without token
    }

    @Test
    void createApplication_duplicate_409() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest();
        ApplicationDTO existing = ApplicationDTO.builder().id(applicationId).status("SAVED").build();
        
        Mockito.when(applicationService.create(any(), eq(jobId), any(), any(), any()))
                .thenThrow(new ApplicationService.DuplicateApplicationException("Conflict", existing));

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateStatus_versionConflict_409() throws Exception {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(ApplicationStatus.APPLIED);
        req.setVersion(1L);

        Mockito.when(applicationService.updateStatus(any(), eq(applicationId), eq(ApplicationStatus.APPLIED), eq(1L)))
                .thenThrow(new jakarta.persistence.OptimisticLockException("Conflict"));

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateStatus_invalidTransition_400() throws Exception {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(ApplicationStatus.SCREENING);
        req.setVersion(1L);

        Mockito.when(applicationService.updateStatus(any(), eq(applicationId), eq(ApplicationStatus.SCREENING), eq(1L)))
                .thenThrow(new IllegalStateException("Invalid transition"));

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void deleteApplication_crossUser_404() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Application not found"))
                .when(applicationService).delete(any(), eq(applicationId));

        mockMvc.perform(delete("/api/v1/applications/" + applicationId))
                .andExpect(status().isNotFound()); // Assumes controller is updated to return 404
    }

    @Test
    void listApplications_sizeExceeded_400() throws Exception {
        Mockito.when(applicationService.listByUser(any(), any(), eq(0), eq(101), any(), any()))
                .thenThrow(new IllegalArgumentException("Page size must not exceed 100"));

        mockMvc.perform(get("/api/v1/applications?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
