package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.dto.response.CourseProgressResponse;
import com.dxh.learninghub.entity.*;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.CourseProgressRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.LessonProgressRepository;
import com.dxh.learninghub.repo.LessonRepository;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LearningProgressServiceImpl implements LearningProgressService {
    LessonRepository lessonRepository;
    LessonProgressRepository lessonProgressRepository;
    CourseProgressRepository courseProgressRepository;
    EnrollmentRepository enrollmentRepository;
    CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void completeLesson(Long lessonId) {

        User user = currentUserProvider.getCurrentUser();

        Lesson lesson = lessonRepository.findWithCourseById(lessonId).orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        Course course = lesson.getChapter().getCourse();

        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        CourseProgress courseProgress = courseProgressRepository
                .findByUserAndCourseForUpdate(user, course)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_NOT_EXISTED));

        Enrollment enrollment = enrollmentRepository.findByUserAndCourse(user, course).filter(item -> List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED).contains(item.getStatus())).orElseThrow(() -> new AppException(ErrorCode.LESSON_ACCESS_DENIED));

        Optional<LessonProgress> lessonProgressOptional = lessonProgressRepository.findByUserAndLesson(user, lesson);

        // Đã hoàn thành thì không làm gì nữa
        if (lessonProgressOptional.isPresent()) {
            LessonProgress lessonProgress = lessonProgressOptional.get();
            if (Boolean.TRUE.equals(lessonProgress.getCompleted())) {
                synchronizeEnrollmentStatus(enrollment, courseProgress);
                return;
            }
            lessonProgress.setCompleted(true);
        } else {
            LessonProgress lessonProgress = LessonProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .completed(true)
                    .build();
            lessonProgressRepository.save(lessonProgress);
        }

        int totalLessons = courseProgress.getTotalLessons();
        int completedLessons = Math.min(courseProgress.getCompletedLessons() + 1, totalLessons);

        courseProgress.setCompletedLessons(completedLessons);

        int progressPercent = totalLessons == 0 ? 0 : completedLessons * 100 / totalLessons;

        courseProgress.setProgressPercent(progressPercent);
        synchronizeEnrollmentStatus(enrollment, courseProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {

        User user = currentUserProvider.getCurrentUser();

        CourseProgress progress = courseProgressRepository.findByUserAndCourseId(user, courseId).orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_NOT_EXISTED));

        return CourseProgressResponse.builder()
                .completedLessons(progress.getCompletedLessons())
                .totalLessons(progress.getTotalLessons())
                .progressPercent(progress.getProgressPercent())
                .completed(progress.getCompleted())
                .completedLessonIds(lessonProgressRepository.findCompletedLessonIds(
                        user.getId(), courseId))
                .build();
    }

    @Override
    @Transactional
    public void synchronizeCourse(Long courseId) {
        int totalLessons = Math.toIntExact(
                lessonRepository.countByChapterCourseId(courseId));

        for (CourseProgress progress :
                courseProgressRepository.findAllByCourseIdForUpdate(courseId)) {
            int completedLessons = Math.toIntExact(
                    lessonProgressRepository
                            .countByUserIdAndLessonChapterCourseIdAndCompletedTrue(
                                    progress.getUser().getId(),
                                    courseId));

            completedLessons = Math.min(completedLessons, totalLessons);
            progress.setTotalLessons(totalLessons);
            progress.setCompletedLessons(completedLessons);
            progress.setProgressPercent(totalLessons == 0
                    ? 0
                    : completedLessons * 100 / totalLessons);

            enrollmentRepository
                    .findByUserAndCourse(progress.getUser(), progress.getCourse())
                    .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE
                            || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                    .ifPresent(enrollment ->
                            synchronizeEnrollmentStatus(enrollment, progress));
        }
    }

    private void synchronizeEnrollmentStatus(Enrollment enrollment, CourseProgress courseProgress) {
        boolean completed = courseProgress.getTotalLessons() > 0
                && courseProgress.getCompletedLessons() >= courseProgress.getTotalLessons();

        courseProgress.setCompleted(completed);
        if (completed) {
            courseProgress.setProgressPercent(100);
        }
        enrollment.setStatus(completed
                ? EnrollmentStatus.COMPLETED
                : EnrollmentStatus.ACTIVE);
    }
}
