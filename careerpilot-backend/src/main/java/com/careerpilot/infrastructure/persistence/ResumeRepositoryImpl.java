package com.careerpilot.infrastructure.persistence;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.persistence.entity.ResumeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MODIFIED for F6: maps new V2 columns (sourceType, sourceJobId,
 * fileKeyPdf, fileKeyDocx, generatedContent, unmatchedKeywords) in both
 * save() and mapToDomain(). Also implements two new interface methods.
 */
@Repository
@RequiredArgsConstructor
public class ResumeRepositoryImpl implements ResumeRepository {

    private final JpaResumeRepository repository;

    @Override
    public Resume save(Resume resume) {
        ResumeEntity entity = ResumeEntity.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .fileKey(resume.getFileKey())
                .originalFilename(resume.getOriginalFilename())
                .parseStatus(resume.getParseStatus())
                .parsedSkills(resume.getParsedSkills())
                .parsedExperience(resume.getParsedExperience())
                .rawText(resume.getRawText())
                .errorMessage(resume.getErrorMessage())
                // F6 fields
                .sourceType(resume.getSourceType() != null ? resume.getSourceType() : "UPLOADED")
                .sourceJobId(resume.getSourceJobId())
                .fileKeyPdf(resume.getFileKeyPdf())
                .fileKeyDocx(resume.getFileKeyDocx())
                .generatedContent(resume.getGeneratedContent())
                .unmatchedKeywords(resume.getUnmatchedKeywords())
                .build();

        ResumeEntity saved = repository.save(entity);
        resume.setId(saved.getId());
        return resume;
    }

    @Override
    public Optional<Resume> findById(UUID id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<Resume> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Resume> findGeneratedByUserIdAndJobId(UUID userId, UUID jobId) {
        return repository
                .findByUserIdAndSourceJobIdAndSourceType(userId, jobId, "GENERATED")
                .map(this::mapToDomain);
    }

    @Override
    public Optional<Resume> findLatestDoneByUserId(UUID userId) {
        return repository
                .findLatestByUserIdAndParseStatus(userId, ParseStatus.DONE)
                .map(this::mapToDomain);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private Resume mapToDomain(ResumeEntity e) {
        return Resume.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .fileKey(e.getFileKey())
                .originalFilename(e.getOriginalFilename())
                .parseStatus(e.getParseStatus())
                .parsedSkills(e.getParsedSkills())
                .parsedExperience(e.getParsedExperience())
                .rawText(e.getRawText())
                .errorMessage(e.getErrorMessage())
                // F6 fields
                .sourceType(e.getSourceType())
                .sourceJobId(e.getSourceJobId())
                .fileKeyPdf(e.getFileKeyPdf())
                .fileKeyDocx(e.getFileKeyDocx())
                .generatedContent(e.getGeneratedContent())
                .unmatchedKeywords(e.getUnmatchedKeywords())
                .build();
    }
}
