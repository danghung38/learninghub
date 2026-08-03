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

    /** Runs every Sunday at 00:00 (Asia/Ho_Chi_Minh). */
    @Scheduled(
            cron = "${jobs.deleted-course-content-cleanup.cron:0 0 0 * * SUN}",
            zone = "${jobs.deleted-course-content-cleanup.zone:Asia/Ho_Chi_Minh}")
    @Transactional
    public void cleanupDeletedCourseContent() {
        try {
            List<Course> deletedCourses = courseRepository.findAll().stream()
                    .filter(course -> course.getStatus() == CourseStatus.DELEDED)
                    .toList();

            int deletedChapters = 0;
            int deletedLessons = 0;
            for (Course course : deletedCourses) {
                // Copy first because deleting a chapter can update the managed collection.
                List<Chapter> chapters = new ArrayList<>(course.getChapters());
                for (Chapter chapter : chapters) {
                    for (Lesson lesson : chapter.getLessons()) {
                        deletedLessons++;
                        // Lesson progress has no orphan cascade, so remove it before the lesson.
                        lessonProgressRepository.deleteAllByLessonId(lesson.getId());
                    }
                    chapterRepository.delete(chapter);
                    deletedChapters++;
                }

                if (!chapters.isEmpty()) {
                    chapterRepository.flush();
                }
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
