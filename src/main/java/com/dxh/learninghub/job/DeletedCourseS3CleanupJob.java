package com.dxh.learninghub.job;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.S3ObjectReferenceRepository;
import com.dxh.learninghub.service.AwsS3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes S3 media owned by soft-deleted courses.
 *
 * <p>The course thumbnail is intentionally never included in the delete set because it is
 * still used by historical records and notifications. The job is idempotent: running it again
 * after the objects have already been deleted is safe for S3.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(
        prefix = "jobs.deleted-course-s3-cleanup",
        name = "enabled",
        havingValue = "true")
public class DeletedCourseS3CleanupJob {

    CourseRepository courseRepository;
    S3ObjectReferenceRepository referenceRepository;
    AwsS3Service awsS3Service;

    @Scheduled(
            cron = "${jobs.deleted-course-s3-cleanup.cron:0 0 0 * * SUN}",
            zone = "${jobs.deleted-course-s3-cleanup.zone:Asia/Ho_Chi_Minh}")
    @Transactional(readOnly = true)
    public void cleanupDeletedCourseResources() {
        try {
            List<Course> deletedCourses = courseRepository.findAllByStatus(CourseStatus.DELEDED);
            if (deletedCourses.isEmpty()) {
                log.info("Deleted-course S3 cleanup: no deleted courses found");
                return;
            }

            // Never remove an object that is still referenced by an active entity elsewhere.
            Set<String> retainedKeys = normalizeKeys(referenceRepository.findRetainedReferences());
            Set<String> thumbnailKeys = deletedCourses.stream()
                    .map(Course::getThumbnail)
                    .map(this::normalizeKey)
                    .filter(key -> key != null && !key.isBlank())
                    .collect(java.util.stream.Collectors.toSet());

            Set<String> resourcesToDelete = new LinkedHashSet<>();
            for (Course course : deletedCourses) {
                addCandidate(resourcesToDelete, course.getVideoUrl());
                course.getChapters().forEach(chapter ->
                        chapter.getLessons().stream()
                                .map(Lesson::getContentUrl)
                                .forEach(contentUrl -> addCandidate(resourcesToDelete, contentUrl)));
            }

            resourcesToDelete.removeAll(thumbnailKeys);
            resourcesToDelete.removeAll(retainedKeys);

            if (resourcesToDelete.isEmpty()) {
                log.info(
                        "Deleted-course S3 cleanup completed: courses={}, deletedObjects=0",
                        deletedCourses.size());
                return;
            }

            awsS3Service.deleteObjects(resourcesToDelete);
            log.info(
                    "Deleted-course S3 cleanup completed: courses={}, deletedObjects={}, thumbnailsKept={}",
                    deletedCourses.size(), resourcesToDelete.size(), thumbnailKeys.size());
        } catch (Exception exception) {
            log.error("Deleted-course S3 cleanup failed", exception);
        }
    }

    private void addCandidate(Set<String> candidates, String rawReference) {
        String key = normalizeKey(rawReference);
        if (key != null && !key.isBlank()) {
            candidates.add(key);
        }
    }

    private Set<String> normalizeKeys(Set<String> references) {
        Set<String> keys = new LinkedHashSet<>();
        if (references == null) {
            return keys;
        }

        references.forEach(reference -> {
            String key = normalizeKey(reference);
            if (key != null && !key.isBlank()) {
                keys.add(key);
            }
        });
        return keys;
    }

    private String normalizeKey(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return null;
        }

        String reference = rawReference.trim();
        if (reference.startsWith("http://") || reference.startsWith("https://")) {
            log.warn("Skipping non-key course S3 reference: {}", reference);
            return null;
        }
        return awsS3Service.normalizeObjectKey(reference);
    }
}
