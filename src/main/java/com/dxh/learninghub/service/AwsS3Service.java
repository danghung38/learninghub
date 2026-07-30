package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.S3ObjectMetadata;
import com.dxh.learninghub.dto.response.PresignedUploadResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private static final int MAX_DELETE_BATCH_SIZE = 1000;

    @Value("${aws.s3.access-key}") private String accessKey;
    @Value("${aws.s3.secret-key}") private String secretKey;
    @Value("${aws.s3.bucket-name}") private String bucketName;
    @Value("${aws.s3.base-prefix}") private String basePrefix;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    private void initS3Clients() {
        var credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        this.s3Client = S3Client.builder().credentialsProvider(credentialsProvider).region(Region.US_EAST_1).build();
        this.s3Presigner = S3Presigner.builder().credentialsProvider(credentialsProvider).region(Region.US_EAST_1).build();
    }

    @Named("normalizeObjectKey")
    public String normalizeObjectKey(String rawUrlOrKey) {
        if (rawUrlOrKey == null || rawUrlOrKey.isBlank()) return rawUrlOrKey;
        String value = rawUrlOrKey.trim();

        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return value.startsWith("/") ? value.substring(1) : value;
        }

        String marker = ".amazonaws.com/";
        int index = value.indexOf(marker);
        if (index != -1 && value.substring(0, index).contains("://" + bucketName + ".s3")) {
            return value.substring(index + marker.length());
        }

        throw new AppException(ErrorCode.INVALID_S3_URL);
    }

    public void deleteFileFromS3(String objectKeyOrUrl) {
        if (objectKeyOrUrl == null || objectKeyOrUrl.isBlank()) return;
        try {
            s3Client.deleteObject(r -> r.bucket(bucketName).key(normalizeObjectKey(objectKeyOrUrl)));
        } catch (Exception e) {
            log.warn("Failed to delete S3 file: {}", objectKeyOrUrl, e);
        }
    }

    public List<S3ObjectMetadata> listObjectsByPrefixes(Collection<String> prefixes) {
        List<S3ObjectMetadata> objects = new ArrayList<>();
        for (String prefix : prefixes) {
            var request = ListObjectsV2Request.builder().bucket(bucketName).prefix(applyBasePrefix(prefix)).build();
            s3Client.listObjectsV2Paginator(request).stream()
                    .flatMap(page -> page.contents().stream())
                    .forEach(obj -> objects.add(new S3ObjectMetadata(obj.key(), obj.lastModified(), obj.size())));
        }
        return objects;
    }

    public void deleteObjects(Collection<String> objectKeys) {
        List<String> keys = objectKeys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();

        for (int start = 0; start < keys.size(); start += MAX_DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> objects = keys.subList(start, Math.min(start + MAX_DELETE_BATCH_SIZE, keys.size()))
                    .stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList();

            var response = s3Client.deleteObjects(r -> r.bucket(bucketName).delete(d -> d.objects(objects).quiet(true)));
            if (!response.errors().isEmpty()) {
                response.errors().forEach(e -> log.error("Failed to delete S3 object {}: {}", e.key(), e.message()));
                throw new IllegalStateException("S3 cleanup batch was partially deleted");
            }
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PresignedUploadResponse generateVideoUploadUrl(String fileName, long fileSize) {
        return generateUploadUrl("videos", FileUploadUtil.validate(fileName, fileSize, UploadPolicy.VIDEO), fileSize);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PresignedUploadResponse generateDocumentUploadUrl(String fileName, long fileSize) {
        return generateUploadUrl("documents", FileUploadUtil.validate(fileName, fileSize, UploadPolicy.DOCUMENT), fileSize);
    }

    // --- Direct upload wrappers ---
    public String uploadAvatar(MultipartFile file, Long userId) { return uploadSmallFile(file, "avatars/" + userId, UploadPolicy.AVATAR); }
    public String uploadTeacherCv(MultipartFile file, Long userId) { return uploadSmallFile(file, "teachers/" + userId + "/cv", UploadPolicy.TEACHER_DOCUMENT); }
    public String uploadTeacherCertificate(MultipartFile file, Long userId) { return uploadSmallFile(file, "teachers/" + userId + "/certificates", UploadPolicy.TEACHER_DOCUMENT); }
    public String uploadCourseThumbnail(MultipartFile file, Long teacherId) { return uploadSmallFile(file, "courses/" + teacherId + "/thumbnails", UploadPolicy.IMAGE); }
    public String uploadWithdrawalPaymentProof(MultipartFile file, Long adminId) { return uploadSmallFile(file, "withdrawals/" + adminId + "/payment-proofs", UploadPolicy.PAYMENT_PROOF); }
    public String uploadAdvertisementImage(MultipartFile file) { return uploadSmallFile(file, "advertisements", UploadPolicy.IMAGE); }

    public String uploadCourseCertificate(byte[] pdf, Long courseId, Long userId, String verificationCode) {
        String objectKey = applyBasePrefix("certificates/" + courseId + "/" + userId + "/" + verificationCode + ".pdf");
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName).key(objectKey)
                    .contentType("application/pdf")
                    .contentDisposition("attachment; filename=\"" + verificationCode + ".pdf\"")
                    .contentLength((long) pdf.length).build();
            s3Client.putObject(request, RequestBody.fromBytes(pdf));
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload certificate {}", verificationCode, e);
            throw new AppException(ErrorCode.UPLOAD_FAIL);
        }
    }

    private String uploadSmallFile(MultipartFile file, String folder, UploadPolicy policy) {
        String fileName = FileUploadUtil.validate(file, policy);
        String objectKey = applyBasePrefix(folder + "/" + UUID.randomUUID() + "_" + fileName);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName).key(objectKey)
                    .contentType(file.getContentType()).contentLength(file.getSize()).build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file to S3 folder {}", folder, e);
            throw new AppException(ErrorCode.UPLOAD_FAIL);
        }
    }

    @Named("resolveFileUrl")
    public String resolveFileUrl(String rawUrlOrKey) {
        if (rawUrlOrKey == null || rawUrlOrKey.isBlank()) return null;
        String value = rawUrlOrKey.trim();
        return (value.startsWith("http://") || value.startsWith("https://")) ? value : generateViewUrl(normalizeObjectKey(value));
    }

    private PresignedUploadResponse generateUploadUrl(String folder, String fileName, long fileSize) {
        String objectKey = applyBasePrefix(folder + "/" + UUID.randomUUID() + "_" + fileName);
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .putObjectRequest(r -> r.bucket(bucketName).key(objectKey).contentLength(fileSize)).build();

        return PresignedUploadResponse.builder()
                .uploadUrl(s3Presigner.presignPutObject(presignRequest).url().toString())
                .fileUrl(objectKey).build();
    }

    @Named("generateViewUrl")
    public String generateViewUrl(String objectKey) {
        return getPresignedGetUrl(objectKey, Duration.ofMinutes(30), null);
    }

    @Named("generateLessonViewUrl")
    public String generateLessonViewUrl(String rawUrlOrKey) {
        if (rawUrlOrKey == null || rawUrlOrKey.isBlank()) return null;
        String objectKey = normalizeObjectKey(rawUrlOrKey);
        String contentType = resolveLessonContentType(objectKey);

        return getPresignedGetUrl(objectKey, Duration.ofMinutes(5), builder -> {
            builder.responseContentDisposition("inline").responseCacheControl("private, max-age=3600");
            if (contentType != null) builder.responseContentType(contentType);
        });
    }

    public String generateDownloadUrl(String objectKey, String fileName) {
        return getPresignedGetUrl(objectKey, Duration.ofMinutes(5), builder ->
                builder.responseContentType("application/pdf").responseContentDisposition("attachment; filename=\"" + fileName + "\"")
        );
    }

    private String getPresignedGetUrl(String objectKey, Duration duration, Consumer<GetObjectRequest.Builder> customizer) {
        if (objectKey == null || objectKey.isBlank()) return null;

        GetObjectRequest.Builder objectRequestBuilder = GetObjectRequest.builder().bucket(bucketName).key(objectKey);
        if (customizer != null) customizer.accept(objectRequestBuilder);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(objectRequestBuilder.build()).build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String resolveLessonContentType(String objectKey) {
        if (objectKey == null) return null;
        int lastDot = objectKey.lastIndexOf('.');
        if (lastDot == -1) return null;

        String extension = objectKey.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> null;
        };
    }

    private String applyBasePrefix(String objectKey) {
        return basePrefix + "/" + (objectKey.startsWith("/") ? objectKey.substring(1) : objectKey);
    }
}