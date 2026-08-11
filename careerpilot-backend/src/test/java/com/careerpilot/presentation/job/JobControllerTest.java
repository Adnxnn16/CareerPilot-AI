package com.careerpilot.presentation.job;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.presentation.config.JwtAuthenticationFilter;
import com.careerpilot.presentation.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(JobController.class)
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock to prevent context failure

    @MockitoBean
    private com.careerpilot.application.job.JobSearchService jobSearchService;

    @MockitoBean
    private com.careerpilot.application.job.JobMatchingService jobMatchingService;

    @BeforeEach
    void setUp() {
        try {
            Mockito.doAnswer(invocation -> {
                HttpServletRequest req = invocation.getArgument(0);
                HttpServletResponse res = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(req, res);
                return null;
            }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @WithMockUser
    void getJob_exists_200() throws Exception {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .title("Software Engineer")
                .company("Tech Corp")
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.title").value("Software Engineer"))
                .andExpect(jsonPath("$.company").value("Tech Corp"));
    }

    @Test
    @WithMockUser
    void getJob_notFound_404() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJob_unauthenticated_401() throws Exception {
        try {
            Mockito.doAnswer(invocation -> {
                HttpServletResponse res = invocation.getArgument(1);
                res.sendError(401, "Unauthorized");
                return null;
            }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mockMvc.perform(get("/api/v1/jobs/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
