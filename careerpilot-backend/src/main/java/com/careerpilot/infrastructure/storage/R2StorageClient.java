package com.careerpilot.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * infrastructure/storage/R2StorageClient.java
 *
 * Cloudflare R2 client using the AWS S3 SDK (R2 is S3-compatible).
 *
 * R2 key conventions:
 *   UPLOADED resumes: resumes/{userId}/{resumeId}/original.{ext}
 *   GENERATED JSON:   generated/{userId}/{resumeId}/resume.json
 *   GENERATED PDF:    generated/{userId}/{resumeId}/resume.pdf
 *   GENERATED DOCX:   generated/{userId}/{resumeId}/resume.docx
 */
@Slf4j
@Component
public class R2StorageClient {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucketName;

    public R2StorageClient(
            @Value("${r2.access-key}") String accessKey,
            @Value("${r2.secret-key}") String secretKey,
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.bucket-name}") String bucketName) {

        this.bucketName = bucketName;
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        URI endpointUri = URI.create(endpoint);

        this.s3Client = S3Client.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();
    }

    // ── Upload methods ────────────────────────────────────────────────────────

    public String uploadUploadedResume(UUID userId, UUID resumeId, byte[] content, String extension) {
        String key = "resumes/" + userId + "/" + resumeId + "/original." + extension;
        upload(key, content, contentTypeFor(extension));
        return key;
    }

    /** F6: Upload the canonical JSON resume document */
    public String uploadGeneratedJson(UUID userId, UUID resumeId, byte[] content) {
        String key = "generated/" + userId + "/" + resumeId + "/resume.json";
        upload(key, content, "application/json");
        return key;
    }

    /** F6: Upload the ATS-rendered PDF */
    public String uploadGeneratedPdf(UUID userId, UUID resumeId, byte[] content) {
        String key = "generated/" + userId + "/" + resumeId + "/resume.pdf";
        upload(key, content, "application/pdf");
        return key;
    }

    /** F6: Upload the ATS-rendered DOCX */
    public String uploadGeneratedDocx(UUID userId, UUID resumeId, byte[] content) {
        String key = "generated/" + userId + "/" + resumeId + "/resume.docx";
        upload(key, content, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return key;
    }

    // ── Presigned URL for frontend direct download ────────────────────────────

    /**
     * Returns a presigned URL valid for 15 minutes.
     * The presentation layer redirects (302) to this URL — avoids proxying
     * bytes through the Spring Boot process.
     */
    public String generatePresignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            log.debug("Deleted R2 object: {}", key);
        } catch (Exception e) {
            log.warn("Failed to delete R2 object {}: {}", key, e.getMessage());
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void upload(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content)
            );
            log.debug("Uploaded to R2: {}", key);
        } catch (Exception e) {
            log.warn("Failed to upload to R2 (key: {}). Ensure R2 credentials are configured if you need cloud storage. Error: {}", key, e.getMessage());
        }
    }

    private String contentTypeFor(String ext) {
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
