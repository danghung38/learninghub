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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(
        prefix = "jobs.deleted-course-content-cleanup",
        name = "enabled",
        havingValue = "true")
public class DeletedCourseContentCleanupJob {

    CourseRepository courseRepository;
    ChapterRepository chapterRepository;
    LessonProgressRepository lessonProgressRepository;
    LearningProgressService learningProgressService;

    // Tự động chạy ngầm dưới background ngay khi server start thành công
    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        log.info("Triggering deleted-course content cleanup on application startup...");
        cleanupDeletedCourseContent();
    }


    /** Runs every Sunday at 00:00 (Asia/Ho_Chi_Minh). */
    @Scheduled(
            cron = "${jobs.deleted-course-content-cleanup.cron:0 0 0 * * SUN}",
            zone = "${jobs.deleted-course-content-cleanup.zone:Asia/Ho_Chi_Minh}")
    @Transactional
    public void cleanupDeletedCourseContent() {
        try {
            List<Course> deletedCourses = courseRepository.findAll().stream()
                    .filter(course -> course.getStatus() == CourseStatus.DELETED)
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

                // 1. BẮT BUỘC: Ngắt liên kết JPA để Hibernate không tự restore lại Chapter
                for (Chapter chapter : chapters) {
                    chapter.setCourse(null);
                }
                course.getChapters().clear();

                // 2. Thực hiện xóa danh sách Chapter khỏi DB
                chapterRepository.deleteAll(chapters);
                chapterRepository.flush();

                // 3. Đồng bộ lại tiến độ
                learningProgressService.synchronizeCourse(course.getId());
            }

            log.info(
                    "Deleted-course content cleanup completed: courses={}, deletedChapters={}, deletedLessons={}",
                    deletedCourses.size(), deletedChapters, deletedLessons);

        } catch (Exception exception) {
            log.error("Deleted-course content cleanup failed", exception);
        }
    }
}
