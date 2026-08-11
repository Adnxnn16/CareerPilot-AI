package com.careerpilot.application.application;

import com.careerpilot.domain.application.Application;
import com.careerpilot.domain.application.ApplicationRepository;
import com.careerpilot.domain.application.ApplicationStatus;
import com.careerpilot.domain.job.Job;
import com.careerpilot.domain.job.JobRepository;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.persistence.JpaApplicationRepository;
import com.careerpilot.infrastructure.persistence.entity.ApplicationEntity;
import com.careerpilot.presentation.application.dto.ApplicationDTO;
import com.careerpilot.presentation.application.dto.BoardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JpaApplicationRepository jpaApplicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testCreate_Success() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job()));
        
        Resume r = new Resume();
        r.setUserId(userId);
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        
        when(applicationRepository.findFirstByUserIdAndJobIdOrderByCreatedAtDesc(userId, jobId))
                .thenReturn(Optional.empty());

        Application saved = new Application();
        saved.setId(UUID.randomUUID());
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationDTO result = applicationService.create(userId, jobId, resumeId, "notes", LocalDate.now());
        
        assertNotNull(result);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testCreate_Duplicate() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job()));
        
        Application existing = new Application();
        existing.setStatus(ApplicationStatus.SAVED);
        
        when(applicationRepository.findFirstByUserIdAndJobIdOrderByCreatedAtDesc(userId, jobId))
                .thenReturn(Optional.of(existing));

        assertThrows(ApplicationService.DuplicateApplicationException.class, () -> {
            applicationService.create(userId, jobId, null, "notes", LocalDate.now());
        });
    }

    @Test
    void testUpdateStatus_Success() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        Application app = new Application();
        app.setUserId(userId);
        app.setStatus(ApplicationStatus.SAVED);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenReturn(app);

        ApplicationDTO result = applicationService.updateStatus(userId, appId, ApplicationStatus.APPLIED, 1L);
        
        assertEquals(ApplicationStatus.APPLIED.name(), result.getStatus());
        assertEquals(1L, app.getVersion());
    }

    @Test
    void testUpdateNotes_Success() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        Application app = new Application();
        app.setUserId(userId);
        
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenReturn(app);

        applicationService.updateNotes(userId, appId, "new notes");
        assertEquals("new notes", app.getNotes());
    }

    @Test
    void testDelete_Success() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        Application app = new Application();
        app.setUserId(userId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        applicationService.delete(userId, appId);
        verify(applicationRepository).deleteById(appId);
    }

    @Test
    void testListByUser() {
        UUID userId = UUID.randomUUID();
        Application app = new Application();
        Page<Application> page = new PageImpl<>(List.of(app));

        when(applicationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        Page<ApplicationDTO> result = applicationService.listByUser(userId, null, 0, 10, "id", "asc");
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetBoard() {
        UUID userId = UUID.randomUUID();
        ApplicationEntity entity = new ApplicationEntity();
        entity.setStatus(ApplicationStatus.SAVED);

        when(jpaApplicationRepository.findAllByUserIdWithJob(userId)).thenReturn(List.of(entity));

        BoardDTO board = applicationService.getBoard(userId);
        assertEquals(1, board.getColumns().get("SAVED").size());
        assertNotNull(board.getColumns().get("APPLIED"));
    }

    @Test
    void testCreate_JobNotFound() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.create(userId, jobId, null, "notes", LocalDate.now()));
    }

    @Test
    void testCreate_ResumeNotFound() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job()));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.create(userId, jobId, resumeId, "notes", LocalDate.now()));
    }

    @Test
    void testCreate_ResumeWrongUser() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job()));
        
        Resume r = new Resume();
        r.setUserId(UUID.randomUUID()); // Different user
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.create(userId, jobId, resumeId, "notes", LocalDate.now()));
    }

    @Test
    void testUpdateStatus_NotFound() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.updateStatus(userId, appId, ApplicationStatus.APPLIED, 1L));
    }

    @Test
    void testUpdateStatus_WrongUser() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Application app = new Application();
        app.setUserId(UUID.randomUUID());
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(SecurityException.class, () -> 
            applicationService.updateStatus(userId, appId, ApplicationStatus.APPLIED, 1L));
    }

    @Test
    void testUpdateStatus_InvalidTransition() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Application app = new Application();
        app.setUserId(userId);
        app.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(IllegalStateException.class, () -> 
            applicationService.updateStatus(userId, appId, ApplicationStatus.INTERVIEW, 1L));
    }

    @Test
    void testListByUser_WithStatusAndDesc() {
        UUID userId = UUID.randomUUID();
        Page<Application> page = new PageImpl<>(List.of(new Application()));
        when(applicationRepository.findByUserIdAndStatus(eq(userId), eq(ApplicationStatus.SAVED), any(Pageable.class))).thenReturn(page);

        Page<ApplicationDTO> result = applicationService.listByUser(userId, ApplicationStatus.SAVED, 0, 10, "id", "desc");
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testListByUser_SizeTooLarge() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.listByUser(userId, null, 0, 101, "id", "asc"));
    }

    @Test
    void testGetBoard_EmptyStatusFallback() {
        UUID userId = UUID.randomUUID();
        ApplicationEntity entity = new ApplicationEntity();
        entity.setStatus(null); // Should fallback to SAVED

        when(jpaApplicationRepository.findAllByUserIdWithJob(userId)).thenReturn(List.of(entity));

        BoardDTO board = applicationService.getBoard(userId);
        assertEquals(1, board.getColumns().get("SAVED").size());
    }
}
