package com.dxh.learninghub.job;

import com.dxh.learninghub.entity.Chapter;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.repo.ChapterRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.LessonProgressRepository;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(
        prefix = "jobs.deleted-course-content-cleanup",
        name = "enabled",
        havingValue = "true")
public class DeletedCourseContentCleanupJob {

    final CourseRepository courseRepository;
    final ChapterRepository chapterRepository;
    final LessonProgressRepository lessonProgressRepository;
    final LearningProgressService learningProgressService;

    @Value("${jobs.deleted-course-content-cleanup.retention-days:30}")
    long retentionDays;

    // Tự động chạy ngầm dưới background ngay khi server start thành công
//    @Async
//    @EventListener(ApplicationReadyEvent.class)
//    @Transactional
//    public void runOnStartup() {
//        log.info("Triggering deleted-course content cleanup on application startup...");
//        cleanupDeletedCourseContent();
//    }

    /** Runs every Sunday at 00:00 (Asia/Ho_Chi_Minh). */
    @Scheduled(
            cron = "${jobs.deleted-course-content-cleanup.cron:0 0 0 * * SUN}",
            zone = "${jobs.deleted-course-content-cleanup.zone:Asia/Ho_Chi_Minh}")
    @Transactional
    public void cleanupDeletedCourseContent() {
        try {
            if (retentionDays < 1) {
                log.warn("Deleted-course cleanup skipped: retention-days must be at least 1");
                return;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<Course> deletedCourses = courseRepository.findAll().stream()
                    .filter(course -> course.getStatus() == CourseStatus.DELETED)
                    .filter(course -> course.getUpdatedAt() != null && course.getUpdatedAt().isBefore(cutoff))
                    .toList();

            if (deletedCourses.isEmpty()) {
                return;
            }

            int deletedChapters = 0;
            int deletedLessons = 0;

            for (Course course : deletedCourses) {
                List<Chapter> chapters = new ArrayList<>(course.getChapters());

                for (Chapter chapter : chapters) {
                    for (Lesson lesson : chapter.getLessons()) {
                        deletedLessons++;
                        lessonProgressRepository.deleteAllByLessonId(lesson.getId());
                    }
                    deletedChapters++;
                }

                // 1. Ngắt liên kết JPA để Hibernate không tự restore lại Chapter
                for (Chapter chapter : chapters) {
                    chapter.setCourse(null);
                }
                course.getChapters().clear();

                // 2. Xóa danh sách Chapter khỏi DB
                chapterRepository.deleteAll(chapters);
                chapterRepository.flush();

                // 3. Đồng bộ lại tiến độ
                learningProgressService.synchronizeCourse(course.getId());
            }

            log.info(
                    "Deleted-course content cleanup completed: courses={}, deletedChapters={}, deletedLessons={}, retentionDays={}",
                    deletedCourses.size(), deletedChapters, deletedLessons, retentionDays);

        } catch (Exception exception) {
            log.error("Deleted-course content cleanup failed", exception);
        }
    }
}