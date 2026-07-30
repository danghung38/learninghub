package com.dxh.learninghub.job;

import com.dxh.learninghub.dto.S3ObjectMetadata;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.repo.S3ObjectReferenceRepository;
import com.dxh.learninghub.service.AwsS3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(prefix = "jobs.s3-orphan-cleanup", name = "enabled", havingValue = "true")
public class S3OrphanCleanupJob {

    static final int MAX_LOG_ITEMS = 100;

    static final List<String> MANAGED_PREFIXES =
            List.of(
                    "images/", "files/", "videos/", "documents/", "avatars/",
                    "teachers/", "courses/", "withdrawals/", "certificates/",
                    "advertisements/");

    final AwsS3Service awsS3Service;
    final S3ObjectReferenceRepository referenceRepository;

    @Value("${jobs.s3-orphan-cleanup.grace-hours:24}")
    long graceHours;

    @Value("${jobs.s3-orphan-cleanup.dry-run:true}")
    boolean dryRun;

    @Scheduled(cron = "${jobs.s3-orphan-cleanup.cron:0 0 3 * * *}", zone = "${jobs.s3-orphan-cleanup.zone:Asia/Ho_Chi_Minh}")
    public void cleanupOrphanObjects() {
        try {
            if (graceHours < 1) {
                log.warn("S3 cleanup skipped: grace-hours must be at least 1");
                return;
            }

            Instant cutoff = Instant.now().minus(graceHours, ChronoUnit.HOURS);

            Set<String> referencedKeys = referenceRepository.findRetainedReferences()
                    .stream()
                    .map(this::normalizeReference)
                    .filter(key -> !key.isBlank())
                    .collect(Collectors.toSet());

            List<S3ObjectMetadata> orphanObjects =
                    awsS3Service.listObjectsByPrefixes(MANAGED_PREFIXES)
                            .stream()
                            .filter(object -> object.lastModified().isBefore(cutoff))
                            .filter(object -> !referencedKeys.contains(object.key()))
                            .toList();

            long totalBytes = orphanObjects.stream().mapToLong(S3ObjectMetadata::size).sum();

            log.info("S3 cleanup scan: referenced={}, orphaned={}, bytes={}, dryRun={}", referencedKeys.size(), orphanObjects.size(), totalBytes, dryRun);

            if (orphanObjects.isEmpty()) {
                return;
            }

            if (dryRun) {
                orphanObjects.stream().limit(MAX_LOG_ITEMS).forEach(object -> log.info("[DRY RUN] Orphan: {}", object.key()));

                if (orphanObjects.size() > MAX_LOG_ITEMS) {
                    log.info("[DRY RUN] {} more objects not logged", orphanObjects.size() - MAX_LOG_ITEMS);
                }

                return;
            }

            awsS3Service.deleteObjects(orphanObjects.stream().map(S3ObjectMetadata::key).toList());

            log.info("Deleted {} orphaned S3 objects", orphanObjects.size());

        } catch (Exception exception) {
            log.error("S3 orphan cleanup failed", exception);
        }
    }

    private String normalizeReference(String reference) {
        try {
            String key = awsS3Service.normalizeObjectKey(reference);
            return key == null ? "" : key;
        } catch (AppException exception) {
            log.warn("Invalid S3 reference in database: {}", reference);
            return "";
        }
    }
}
