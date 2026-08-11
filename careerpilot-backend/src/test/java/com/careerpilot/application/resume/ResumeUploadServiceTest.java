package com.careerpilot.application.resume;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.ai.OpenAIClient;
import com.careerpilot.infrastructure.document.DocumentParser;
import com.careerpilot.infrastructure.storage.R2StorageClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests updated after the @Async self-invocation fix (F2 / ResumeParsingWorker).
 *
 * Architecture post-fix:
 *   ResumeUploadService  → orchestrates MIME validation, R2 upload, PENDING record
 *   ResumeParsingWorker  → owns @Async processResumeAsync (Tika + OpenAI pipeline)
 *
 * The initiateUpload tests stub resumeRepository.findById so the worker (running
 * synchronously in tests without a Spring context) can find the persisted record.
 *
 * The processResumeAsync tests target ResumeParsingWorker directly.
 */
@ExtendWith(MockitoExtension.class)
class ResumeUploadServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private R2StorageClient   r2StorageClient;
    @Mock private DocumentParser    documentParser;
    @Mock private OpenAIClient      openAIClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResumeParsingWorker  resumeParsingWorker;
    private ResumeUploadService  resumeUploadService;

    @BeforeEach
    void setUp() {
        resumeParsingWorker = new ResumeParsingWorker(
                resumeRepository, documentParser, openAIClient, objectMapper);

        resumeUploadService = new ResumeUploadService(
                resumeRepository, r2StorageClient, documentParser, resumeParsingWorker);
    }

    // ── initiateUpload tests ──────────────────────────────────────────────────

    @Test
    void testInitiateUpload_Success_Pdf() throws Exception {
        UUID userId    = UUID.randomUUID();
        byte[] fileBytes = "test".getBytes();
        String fileKey = "key";

        when(documentParser.detectMimeType(fileBytes, "test.pdf")).thenReturn("application/pdf");
        when(r2StorageClient.uploadUploadedResume(eq(userId), nullable(UUID.class), any(byte[].class), eq("pdf")))
                .thenReturn(fileKey);

        // The worker runs synchronously in tests; stub findById so it can locate the record
        Resume savedResume = new Resume();
        savedResume.setUserId(userId);
        savedResume.setParseStatus(ParseStatus.PENDING);
        when(resumeRepository.findById(nullable(UUID.class))).thenReturn(Optional.of(savedResume));
        when(documentParser.parseToString(any(InputStream.class))).thenReturn("raw text");
        when(openAIClient.parseResume("raw text"))
                .thenReturn("{\"skills\":[\"Java\"],\"experience\":[]}");

        UUID resumeId = resumeUploadService.initiateUpload(userId, "test.pdf", fileBytes);

        verify(resumeRepository, atLeast(2)).save(any(Resume.class));
        assertEquals(ParseStatus.DONE, savedResume.getParseStatus());
    }

    @Test
    void testInitiateUpload_Success_Docx() throws Exception {
        UUID userId    = UUID.randomUUID();
        byte[] fileBytes = "test".getBytes();
        String fileKey = "key";

        when(documentParser.detectMimeType(fileBytes, "test.docx"))
                .thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(r2StorageClient.uploadUploadedResume(eq(userId), nullable(UUID.class), any(byte[].class), eq("docx")))
                .thenReturn(fileKey);

        Resume savedResume = new Resume();
        savedResume.setUserId(userId);
        savedResume.setParseStatus(ParseStatus.PENDING);
        when(resumeRepository.findById(nullable(UUID.class))).thenReturn(Optional.of(savedResume));
        when(documentParser.parseToString(any(InputStream.class))).thenReturn("raw text");
        when(openAIClient.parseResume("raw text"))
                .thenReturn("{\"skills\":[\"Java\"],\"experience\":[]}");

        UUID resumeId = resumeUploadService.initiateUpload(userId, "test.docx", fileBytes);

        verify(resumeRepository, atLeast(2)).save(any(Resume.class));
    }

    @Test
    void testInitiateUpload_InvalidType() {
        UUID userId    = UUID.randomUUID();
        byte[] fileBytes = "test".getBytes();

        when(documentParser.detectMimeType(fileBytes, "test.txt")).thenReturn("text/plain");

        assertThrows(IllegalArgumentException.class,
                () -> resumeUploadService.initiateUpload(userId, "test.txt", fileBytes));
    }

    // ── processResumeAsync tests — target ResumeParsingWorker directly ─────────

    @Test
    void testProcessResumeAsync_Success() throws Exception {
        UUID resumeId  = UUID.randomUUID();
        byte[] fileBytes = "test".getBytes();

        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setParseStatus(ParseStatus.PENDING);

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(documentParser.parseToString(any(InputStream.class))).thenReturn("raw text");
        when(openAIClient.parseResume("raw text")).thenReturn("{\"skills\":[\"Java\"],\"experience\":[]}");

        resumeParsingWorker.processResumeAsync(resumeId, fileBytes);

        assertEquals(ParseStatus.DONE, resume.getParseStatus());
        assertEquals("[\"Java\"]", resume.getParsedSkills());
        assertEquals("[]", resume.getParsedExperience());
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessResumeAsync_NotFound() {
        UUID resumeId = UUID.randomUUID();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> resumeParsingWorker.processResumeAsync(resumeId, "test".getBytes()));
    }

    @Test
    void testProcessResumeAsync_Failure() throws Exception {
        UUID resumeId  = UUID.randomUUID();
        byte[] fileBytes = "test".getBytes();

        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setParseStatus(ParseStatus.PENDING);

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(documentParser.parseToString(any(InputStream.class)))
                .thenThrow(new RuntimeException("Parsing failed"));

        resumeParsingWorker.processResumeAsync(resumeId, fileBytes);

        assertEquals(ParseStatus.FAILED, resume.getParseStatus());
        assertEquals("Parsing failed", resume.getErrorMessage());
        verify(resumeRepository, times(2)).save(resume);
    }
}
