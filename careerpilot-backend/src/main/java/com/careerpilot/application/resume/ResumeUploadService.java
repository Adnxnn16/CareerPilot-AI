package com.careerpilot.application.resume;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.document.DocumentParser;
import com.careerpilot.infrastructure.storage.R2StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeUploadService {

    private final ResumeRepository resumeRepository;
    private final R2StorageClient r2StorageClient;
    private final DocumentParser documentParser;
    private final ResumeParsingWorker resumeParsingWorker; // separate bean — proxy fires @Async correctly

    /**
     * Initialises the upload synchronously and returns the PENDING resume ID immediately (202 Accepted).
     * Heavy parsing is delegated to {@link ResumeParsingWorker#processResumeAsync} which runs on the
     * {@code resumeTaskExecutor} thread pool via Spring's AOP proxy.
     */
    public UUID initiateUpload(UUID userId, String originalFilename, byte[] fileBytes) {
        String mimeType = documentParser.detectMimeType(fileBytes, originalFilename);
        if (!mimeType.equals("application/pdf") &&
            !mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF and DOCX are allowed.");
        }

        String extension = mimeType.equals("application/pdf") ? "pdf" : "docx";

        // Persist PENDING record first so the ID exists before parsing begins
        Resume pendingResume = Resume.builder()
                .userId(userId)
                .sourceType("UPLOADED")
                .originalFilename(originalFilename)
                .parseStatus(ParseStatus.PENDING)
                .build();

        resumeRepository.save(pendingResume);

        // Upload raw bytes to R2, then update the fileKey on the record
        String fileKey = r2StorageClient.uploadUploadedResume(userId, pendingResume.getId(), fileBytes, extension);
        pendingResume.setFileKey(fileKey);
        resumeRepository.save(pendingResume);

        // Calling through the injected bean (not `this.`) means Spring's proxy intercepts the call
        // and dispatches it to resumeTaskExecutor — this is what makes @Async actually work.
        resumeParsingWorker.processResumeAsync(pendingResume.getId(), fileBytes);

        return pendingResume.getId();
    }
}
