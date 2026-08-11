package com.careerpilot.application.job;

import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.infrastructure.job.AdzunaClient;
import com.careerpilot.presentation.job.dto.JobSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSearchServiceTest {

    @Mock
    private AdzunaClient adzunaClient;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private JobSearchService jobSearchService;

    @Test
    void testSearchJobs_CacheHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<JobSearchResponse> cachedJobs = List.of(new JobSearchResponse());
        when(valueOperations.get(anyString())).thenReturn(cachedJobs);

        List<JobSearchResponse> result = jobSearchService.searchJobs("dev", "ny", 1, 10);
        assertEquals(1, result.size());
        verify(adzunaClient, never()).searchJobs(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testSearchJobs_CacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        JobSearchResponse jobDto = new JobSearchResponse();
        jobDto.setExternalId("ext1");
        jobDto.setTitle("Dev");
        when(adzunaClient.searchJobs(anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of(jobDto));
        
        when(jobRepository.findByExternalId("ext1")).thenReturn(Optional.empty());

        List<JobSearchResponse> result = jobSearchService.searchJobs("dev", "ny", 1, 10);
        assertEquals(1, result.size());
        assertEquals("ext1", result.get(0).getExternalId());
        
        verify(jobRepository).save(any(Job.class));
        verify(valueOperations).set(anyString(), any(), eq(Duration.ofHours(1)));
    }

    @Test
    void testSearchJobs_CacheMiss_ExistingJob() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        JobSearchResponse jobDto = new JobSearchResponse();
        jobDto.setExternalId("ext1");
        jobDto.setTitle("Dev");
        when(adzunaClient.searchJobs(anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of(jobDto));
        
        Job existingJob = new Job();
        existingJob.setExternalId("ext1");
        when(jobRepository.findByExternalId("ext1")).thenReturn(Optional.of(existingJob));

        List<JobSearchResponse> result = jobSearchService.searchJobs("dev", "ny", 1, 10);
        assertEquals(1, result.size());
        
        verify(jobRepository, never()).save(any(Job.class));
    }
}
