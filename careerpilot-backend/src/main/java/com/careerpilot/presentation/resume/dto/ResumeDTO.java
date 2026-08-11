package com.careerpilot.presentation.resume.dto;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * presentation/resume/dto/ResumeDTO.java — MODIFIED for F6
 *
 * Extended with F6 fields: sourceType, sourceJobId, fileKeyPdf,
 * fileKeyDocx, unmatchedKeywords.
 * Frontend uses sourceType to distinguish uploaded vs generated resumes
 * without a new endpoint.
 * Controllers NEVER return ResumeEntity — only this DTO.
 */
@Data
public class ResumeDTO {

    // V1 fields
    private UUID id;
    private UUID userId;
    private String originalFilename;
    private ParseStatus status;
    private String errorMessage;

    // F2: parsed content (only populated for UPLOADED + DONE resumes)
    private String parsedSkills;
    private String parsedExperience;

    // F6: discriminator and generated-resume metadata
    private String sourceType;          // "UPLOADED" | "GENERATED"
    private UUID sourceJobId;           // non-null for GENERATED
    private String fileKeyPdf;          // R2 key for download (GENERATED + DONE)
    private String fileKeyDocx;         // R2 key for download (GENERATED + DONE)
    private List<String> unmatchedKeywords; // JD gap report (GENERATED + DONE)

    // ── Factory ──────────────────────────────────────────────────────────────

    public static ResumeDTO from(Resume r) {
        ResumeDTO dto = new ResumeDTO();
        dto.setId(r.getId());
        dto.setUserId(r.getUserId());
        dto.setOriginalFilename(r.getOriginalFilename());
        dto.setStatus(r.getParseStatus());
        dto.setErrorMessage(r.getErrorMessage());
        dto.setParsedSkills(r.getParsedSkills());
        dto.setParsedExperience(r.getParsedExperience());
        // F6 fields
        dto.setSourceType(r.getSourceType() != null ? r.getSourceType() : "UPLOADED");
        dto.setSourceJobId(r.getSourceJobId());
        dto.setFileKeyPdf(r.getFileKeyPdf());
        dto.setFileKeyDocx(r.getFileKeyDocx());
        dto.setUnmatchedKeywords(r.getUnmatchedKeywords());
        return dto;
    }
}
