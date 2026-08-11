package com.careerpilot.domain.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Domain model for a resume — covers both UPLOADED and GENERATED source types.
 * Zero Spring/JPA annotations — this is a pure Java object.
 *
 * F6 additions (V2 migration):
 *  - sourceType:         "UPLOADED" | "GENERATED"
 *  - sourceJobId:        UUID of the job this was tailored for (GENERATED only)
 *  - fileKeyPdf:         R2 key for generated PDF (GENERATED + DONE only)
 *  - fileKeyDocx:        R2 key for generated DOCX (GENERATED + DONE only)
 *  - generatedContent:   Canonical JSON resume string produced by OpenAI
 *  - unmatchedKeywords:  JD keywords the AI could not honestly match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
    // ── Shared fields (V1) ──────────────────────────────────────────────────
    private UUID id;
    private UUID userId;

    /** R2 key of the uploaded file (UPLOADED) or JSON artifact (GENERATED). Nullable for GENERATED. */
    private String fileKey;

    /** Original user-provided filename. Nullable for GENERATED resumes. */
    private String originalFilename;

    private ParseStatus parseStatus;
    private String parsedSkills;      // JSON string
    private String parsedExperience;  // JSON string
    private String rawText;
    private String errorMessage;

    // ── F6 additions (V2) ───────────────────────────────────────────────────
    /** Discriminator: "UPLOADED" or "GENERATED". Never null. */
    private String sourceType;

    /** The job this resume was tailored for. Null for UPLOADED resumes. */
    private UUID sourceJobId;

    /** R2 key → generated/{userId}/{id}/resume.pdf */
    private String fileKeyPdf;

    /** R2 key → generated/{userId}/{id}/resume.docx */
    private String fileKeyDocx;

    /** Full structured JSON resume as returned and validated from OpenAI. */
    private String generatedContent;

    /** JD keywords the AI flagged as unaddressable given the candidate's real experience. */
    private List<String> unmatchedKeywords;

    // ── Convenience factory methods ─────────────────────────────────────────
    public boolean isGenerated() {
        return "GENERATED".equals(sourceType);
    }

    public boolean isUploaded() {
        return "UPLOADED".equals(sourceType);
    }
}
