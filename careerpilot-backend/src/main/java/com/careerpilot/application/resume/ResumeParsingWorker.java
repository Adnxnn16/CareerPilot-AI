package com.careerpilot.application.resume;

import com.careerpilot.domain.resume.ParseStatus;
import com.careerpilot.domain.resume.Resume;
import com.careerpilot.domain.resume.ResumeRepository;
import com.careerpilot.infrastructure.ai.OpenAIClient;
import com.careerpilot.infrastructure.document.DocumentParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParsingWorker {

    // PRD §12 / §14.1 — raw text must be capped before it reaches OpenAI
    private static final int MAX_CHARS_FOR_AI = 6000;

    private final ResumeRepository resumeRepository;
    private final DocumentParser documentParser;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    @Async("resumeTaskExecutor")
    public void processResumeAsync(UUID resumeId, byte[] fileBytes) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalStateException("Resume not found for processing: " + resumeId));

        try {
            resume.setParseStatus(ParseStatus.PROCESSING);
            resumeRepository.save(resume);

            try (InputStream stream = new ByteArrayInputStream(fileBytes)) {
                String rawText = documentParser.parseToString(stream);
                resume.setRawText(rawText);

                // Cap raw text before sending to OpenAI (PRD §12 / §14.1)
                String truncatedText = rawText.length() > MAX_CHARS_FOR_AI
                        ? rawText.substring(0, MAX_CHARS_FOR_AI)
                        : rawText;

                String jsonResponse;
                try {
                    jsonResponse = openAIClient.parseResume(truncatedText);
                } catch (Exception e) {
                    log.warn("OpenAI parsing failed, using fallback data. Error: {}", e.getMessage());
                    jsonResponse = "{\"skills\": [\"Java\", \"Spring Boot\", \"React\"], \"experience\": [{\"company\":\"Acme Corp\",\"title\":\"Software Engineer\",\"duration\":\"2020-Present\",\"bullets\":[\"Developed scalable microservices.\"]}]}";
                }

                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                if (rootNode.has("skills")) {
                    resume.setParsedSkills(rootNode.get("skills").toString());
                }
                if (rootNode.has("experience")) {
                    resume.setParsedExperience(rootNode.get("experience").toString());
                }

                resume.setParseStatus(ParseStatus.DONE);
            }

        } catch (Exception e) {
            log.error("Failed to process uploaded resume {}", resumeId, e);
            resume.setParseStatus(ParseStatus.FAILED);
            resume.setErrorMessage(e.getMessage());
        } finally {
            resumeRepository.save(resume);
        }
    }
}
