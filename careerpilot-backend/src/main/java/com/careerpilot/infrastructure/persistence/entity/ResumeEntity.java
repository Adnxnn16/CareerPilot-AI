package com.careerpilot.infrastructure.persistence.entity;

import com.careerpilot.domain.resume.ParseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the resumes table.
 * MODIFIED for F6 (V2 migration): added sourceType, sourceJobId,
 * fileKeyPdf, fileKeyDocx, generatedContent, unmatchedKeywords.
 * Also: dropped nullable=false from fileKey and originalFilename.
 */
@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    // nullable=false removed for F6 — GENERATED resumes use generated_content
    @Column
    private String fileKey;

    // nullable=false removed for F6 — GENERATED resumes have no original filename
    @Column
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParseStatus parseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    private String parsedSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    private String parsedExperience;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    private String errorMessage;

    // ── F6 additions ─────────────────────────────────────────────────────────

    /** "UPLOADED" or "GENERATED". Default enforced by DB but also set in code. */
    @Column(nullable = false)
    @Builder.Default
    private String sourceType = "UPLOADED";

    /** FK to jobs.id — only populated for GENERATED resumes. */
    @Column
    private UUID sourceJobId;

    /** R2 key for the rendered PDF (GENERATED + DONE only). */
    @Column
    private String fileKeyPdf;

    /** R2 key for the rendered DOCX (GENERATED + DONE only). */
    @Column
    private String fileKeyDocx;

    /** Canonical JSON resume string produced and validated by ResumeTailoringService. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String generatedContent;

    /** JD keywords flagged as unaddressable without fabricating experience. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> unmatchedKeywords;
}
