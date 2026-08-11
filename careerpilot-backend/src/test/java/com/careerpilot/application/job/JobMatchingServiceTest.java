package com.careerpilot.application.job;

import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.ai.OpenAIClient;
import com.careerpilot.infrastructure.cache.CacheService;
import com.careerpilot.presentation.job.dto.JobMatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobMatchingServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private OpenAIClient openAIClient;
    @Mock
    private CacheService cacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JobMatchingService jobMatchingService;

    @BeforeEach
    void setUp() {
        jobMatchingService = new JobMatchingService(
                jobRepository, resumeRepository, openAIClient, cacheService, objectMapper);
    }

    @Test
    void testMatchJob_CacheHit() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(cacheService.getCachedMatch(userId, jobId)).thenReturn(new JobMatchResponse());

        JobMatchResponse result = jobMatchingService.matchJob(userId, jobId);

        assertNotNull(result);
        // Cache hit — no DB or OpenAI calls needed
        verify(resumeRepository, never()).findLatestDoneByUserId(any());
        verify(openAIClient, never()).matchJob(any(), any());
    }

    @Test
    void testMatchJob_NoResume() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(cacheService.getCachedMatch(userId, jobId)).thenReturn(null);
        when(resumeRepository.findLatestDoneByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> jobMatchingService.matchJob(userId, jobId));
    }

    @Test
    void testMatchJob_ResumeNotDone() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(cacheService.getCachedMatch(userId, jobId)).thenReturn(null);

        Resume resume = new Resume();
        resume.setParseStatus(ParseStatus.PENDING);
        when(resumeRepository.findLatestDoneByUserId(userId)).thenReturn(Optional.of(resume));

        assertThrows(IllegalArgumentException.class, () -> jobMatchingService.matchJob(userId, jobId));
    }

    @Test
    void testMatchJob_JobNotFound() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(cacheService.getCachedMatch(userId, jobId)).thenReturn(null);

        Resume resume = new Resume();
        resume.setParseStatus(ParseStatus.DONE);
        when(resumeRepository.findLatestDoneByUserId(userId)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> jobMatchingService.matchJob(userId, jobId));
    }
}
