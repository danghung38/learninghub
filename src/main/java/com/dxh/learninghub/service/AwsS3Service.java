package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.S3ObjectMetadata;
import com.dxh.learninghub.dto.response.PresignedUploadResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.utils.CurrentUserProvider;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private static final int MAX_DELETE_BATCH_SIZE = 1000;
    private static final Region S3_REGION = Region.AP_SOUTHEAST_1;
    private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(59);
    private static final Duration COURSE_VIDEO_URL_DURATION = Duration.ofDays(7);
    private static final Duration LESSON_VIDEO_URL_DURATION = Duration.ofHours(2);
    private static final String LESSON_CACHE_CONTROL = "public, max-age=604800, immutable";
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mkv", "mov", "m4v", "avi");
    private final CurrentUserProvider user;
    @Value("${aws.s3.access-key}")
    private String accessKey;
    @Value("${aws.s3.secret-key}")
    private String secretKey;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    @Value("${aws.s3.base-prefix}")
    private String basePrefix;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    private void initS3Clients() {
        var credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        this.s3Client = S3Client.builder().credentialsProvider(credentialsProvider).region(S3_REGION).build();
        this.s3Presigner = S3Presigner.builder().credentialsProvider(credentialsProvider).region(S3_REGION).build();
    }

    // --- Core Direct Upload
    public String uploadFile(MultipartFile file, String folder, UploadPolicy policy) {
        String fileName = FileUploadUtil.validate(file, policy);
        String objectKey = createObjectKey(folder, fileName);
        try {
            PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(objectKey).contentType(file.getContentType()).contentLength(file.getSize()).build();

            // Sửa thành fromBytes để tránh lỗi lệch content-length
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file to S3 folder {}", folder, e);
            throw new AppException(ErrorCode.UPLOAD_FAIL);
        }
    }

    // --- Presigned Uploads ---
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PresignedUploadResponse generateVideoUploadUrl(String fileName, long fileSize) {
        return generateUploadUrl("videos", FileUploadUtil.validate(fileName, fileSize, UploadPolicy.VIDEO), fileSize);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PresignedUploadResponse generateDocumentUploadUrl(String fileName, long fileSize) {
        return generateUploadUrl("documents", FileUploadUtil.validate(fileName, fileSize, UploadPolicy.DOCUMENT), fileSize);
    }

    @PreAuthorize("isAuthenticated()")
    public PresignedUploadResponse generateChatUploadUrl(String fileName, long fileSize) {
        String chatFolder = "imagechat/" + user.getCurrentUser().getId();

        return generateUploadUrl(chatFolder, FileUploadUtil.validate(fileName, fileSize, UploadPolicy.IMAGE), fileSize);
    }

    private PresignedUploadResponse generateUploadUrl(String folder, String fileName, long fileSize) {
        String objectKey = createObjectKey(folder, fileName);
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder().signatureDuration(UPLOAD_URL_DURATION).putObjectRequest(r -> r.bucket(bucketName).key(objectKey).contentLength(fileSize)).build();

        return PresignedUploadResponse.builder().uploadUrl(s3Presigner.presignPutObject(presignRequest).url().toString()).fileUrl(objectKey).build();
    }

    // --- Read URLs ---
    @Named("resolveFileUrl")
    public String resolveFileUrl(String rawUrlOrKey) {
        return resolveObjectUrl(rawUrlOrKey, this::getPermanentObjectUrl);
    }

    @Named("resolveVideoUrl")
    public String resolveVideoUrl(String rawUrlOrKey) {
        return resolveObjectUrl(rawUrlOrKey, this::generateViewUrl);
    }

    @Named("generateViewUrl")
    public String generateViewUrl(String objectKey) {
        return getPresignedGetUrl(objectKey, COURSE_VIDEO_URL_DURATION, null);
    }

    @Named("generateLessonViewUrl")
    public String generateLessonViewUrl(String rawUrlOrKey) {
        return resolveObjectUrl(rawUrlOrKey, this::generateLessonObjectUrl);
    }

    private String generateLessonObjectUrl(String objectKey) {
        if (!isVideoObject(objectKey)) {
            return getPermanentObjectUrl(objectKey);
        }

        String contentType = resolveLessonContentType(objectKey);

        return getPresignedGetUrl(objectKey, LESSON_VIDEO_URL_DURATION, builder -> {
            builder.responseContentDisposition("inline").responseCacheControl(LESSON_CACHE_CONTROL);
            if (contentType != null) {
                builder.responseContentType(contentType);
            }
        });
    }

    private String resolveObjectUrl(String rawUrlOrKey, Function<String, String> objectKeyResolver) {
        if (rawUrlOrKey == null || rawUrlOrKey.isBlank()) return null;

        String value = rawUrlOrKey.trim();
        return value.startsWith("http") ? value : objectKeyResolver.apply(normalizeObjectKey(value));
    }

    private String getPresignedGetUrl(String objectKey, Duration duration, Consumer<GetObjectRequest.Builder> customizer) {
        if (objectKey == null || objectKey.isBlank()) return null;

        GetObjectRequest.Builder objectRequestBuilder = GetObjectRequest.builder().bucket(bucketName).key(objectKey);
        if (customizer != null) customizer.accept(objectRequestBuilder);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(duration).getObjectRequest(objectRequestBuilder.build()).build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String getPermanentObjectUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        return s3Client.utilities().getUrl(request -> request.bucket(bucketName).key(objectKey)).toExternalForm();
    }

    private boolean isVideoObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return false;

        String path = objectKey.toLowerCase(Locale.ROOT);
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) path = path.substring(0, queryIndex);
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) path = path.substring(0, fragmentIndex);
        int extensionIndex = path.lastIndexOf('.');

        return extensionIndex >= 0 && VIDEO_EXTENSIONS.contains(path.substring(extensionIndex + 1));
    }

    // --- Deletion & Querying ---
    public void deleteFileFromS3(String objectKeyOrUrl) {
        if (objectKeyOrUrl == null || objectKeyOrUrl.isBlank()) return;
        try {
            s3Client.deleteObject(r -> r.bucket(bucketName).key(normalizeObjectKey(objectKeyOrUrl)));
        } catch (Exception e) {
            log.warn("Failed to delete S3 file: {}", objectKeyOrUrl, e);
        }
    }

    public void deleteObjects(Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) return;

        List<String> keys = objectKeys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();

        for (int start = 0; start < keys.size(); start += MAX_DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> objects = keys.subList(start, Math.min(start + MAX_DELETE_BATCH_SIZE, keys.size())).stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList();

            var response = s3Client.deleteObjects(r -> r.bucket(bucketName).delete(d -> d.objects(objects).quiet(true)));
            if (!response.errors().isEmpty()) {
                response.errors().forEach(e -> log.error("Failed to delete S3 object {}: {}", e.key(), e.message()));
                throw new IllegalStateException("S3 cleanup batch was partially deleted");
            }
        }
    }

    public List<S3ObjectMetadata> listObjectsByPrefixes(Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) return Collections.emptyList();

        List<S3ObjectMetadata> objects = new ArrayList<>();
        for (String prefix : prefixes) {
            var request = ListObjectsV2Request.builder().bucket(bucketName).prefix(buildObjectKey(prefix, "")).build();
            s3Client.listObjectsV2Paginator(request).stream().flatMap(page -> page.contents().stream()).forEach(obj -> objects.add(new S3ObjectMetadata(obj.key(), obj.lastModified(), obj.size())));
        }
        return objects;
    }

    // --- Helpers ---
    @Named("normalizeObjectKey")
    public String normalizeObjectKey(String rawUrlOrKey) {
        if (rawUrlOrKey == null || rawUrlOrKey.isBlank()) return rawUrlOrKey;
        String value = rawUrlOrKey.trim();
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private String buildObjectKey(String folder, String fileName) {
        String cleanFolder = folder.replaceAll("^/+|/+$", "");
        return basePrefix + "/" + cleanFolder + (fileName.isBlank() ? "" : "/" + fileName);
    }

    private String createObjectKey(String folder, String validatedFileName) {
        return buildObjectKey(folder, UUID.randomUUID() + "_" + validatedFileName);
    }

    private String resolveLessonContentType(String objectKey) {
        if (objectKey == null) return null;
        return MediaTypeFactory.getMediaType(objectKey).map(Object::toString).orElse(null);
    }
}
