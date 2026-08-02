package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.request.LessonRequest;
import com.dxh.learninghub.dto.request.LessonUpdateRequest;
import com.dxh.learninghub.dto.response.LessonResponse;
import com.dxh.learninghub.entity.Chapter;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.LessonMapper;
import com.dxh.learninghub.repo.ChapterRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.LessonProgressRepository;
import com.dxh.learninghub.repo.LessonRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import com.dxh.learninghub.service.interfac.LessonService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonServiceImpl implements LessonService {

    LessonRepository lessonRepository;
    ChapterRepository chapterRepository;
    LessonMapper lessonMapper;
    CurrentUserProvider currentUserProvider;
    AwsS3Service awsS3Service;
    LessonProgressRepository lessonProgressRepository;
    LearningProgressService learningProgressService;
    EnrollmentRepository enrollmentRepository;

    static final List<EnrollmentStatus> LEARNING_STATUSES =
            List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public LessonResponse createLesson(LessonRequest request) {
        Chapter chapter = getChapterAndValidateOwner(request.chapterId());
        markAsDraft(chapter.getCourse());

        Lesson lesson = lessonMapper.lessonRequestToLesson(request);
        lesson.setChapter(chapter);

        lesson.setContentUrl(awsS3Service.normalizeObjectKey(request.contentUrl()));

        lessonRepository.saveAndFlush(lesson);
        learningProgressService.synchronizeCourse(chapter.getCourse().getId());

        return lessonMapper.lessonToLessonResponse(lesson);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public LessonResponse updateLesson(Long id, LessonUpdateRequest request) {

        Lesson lesson = getLessonOrThrow(id);

        validateOwner(lesson.getChapter());

        if (request.chapterId() != null
                && !Objects.equals(lesson.getChapter().getId(), request.chapterId())) {
            Chapter targetChapter = getChapterAndValidateOwner(request.chapterId());
            Long currentCourseId = lesson.getChapter().getCourse().getId();
            Long targetCourseId = targetChapter.getCourse().getId();

            if (!Objects.equals(currentCourseId, targetCourseId)) {
                throw new AppException(ErrorCode.LESSON_CROSS_COURSE_MOVE_NOT_ALLOWED);
            }
            lesson.setChapter(targetChapter);
        }
        markAsDraft(lesson.getChapter().getCourse());

        String oldObjectKey = awsS3Service.normalizeObjectKey(lesson.getContentUrl());
        lessonMapper.updateLessonFromRequest(request, lesson);
        lessonRepository.save(lesson);

        if (oldObjectKey != null
                && !Objects.equals(oldObjectKey, lesson.getContentUrl())) {
            awsS3Service.deleteFileFromS3(oldObjectKey);
        }

        return lessonMapper.lessonToLessonResponse(lesson);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public void deleteLesson(Long id) {
        Lesson lesson = getLessonOrThrow(id);

        Long courseId = lesson.getChapter().getCourse().getId();

        validateOwner(lesson.getChapter());
        markAsDraft(lesson.getChapter().getCourse());

        String objectKey = lesson.getContentUrl();
        lessonProgressRepository.deleteAllByLessonId(lesson.getId());
        lessonRepository.delete(lesson);
        lessonRepository.flush();
        learningProgressService.synchronizeCourse(courseId);
        deleteLessonFileAfterCommit(objectKey);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public LessonResponse getLessonById(Long id) {
        Lesson lesson = getLessonOrThrow(id);
        validateLessonAccess(lesson.getChapter().getCourse());
        return lessonMapper.lessonToLessonResponse(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<LessonResponse> getLessonsByChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findWithCourseAndAuthorById(chapterId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_EXISTED));

        validateLessonAccess(chapter.getCourse());

        return lessonRepository.findByChapterOrderByIdAsc(chapter).stream()
                .map(lessonMapper::lessonToLessonResponse)
                .toList();
    }

    private Lesson getLessonOrThrow(Long id) {
        return lessonRepository.findWithCourseById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
    }

    private Chapter getChapterAndValidateOwner(Long chapterId) {
        Chapter chapter = chapterRepository.findWithCourseAndAuthorById(chapterId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_EXISTED));

        validateOwner(chapter);

        return chapter;
    }

    private void validateOwner(Chapter chapter) {
        User currentUser = currentUserProvider.getCurrentUser();
        User author = chapter.getCourse().getAuthor();

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));

        if (!isAdmin && !Objects.equals(currentUser.getId(), author.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!isAdmin && chapter.getCourse().getStatus() == CourseStatus.DELEDED) {
            throw new AppException(ErrorCode.COURSE_DELEDED_READ_ONLY);
        }
    }

    private void validateLessonAccess(Course course) {
        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        User currentUser = currentUserProvider.getCurrentUser();
        boolean isEnrolled = enrollmentRepository.existsByUserAndCourseAndStatusIn(
                currentUser,
                course,
                LEARNING_STATUSES
        );

        if (!isEnrolled) {
            throw new AppException(ErrorCode.LESSON_ACCESS_DENIED);
        }
    }

    private void markAsDraft(Course course) {
        if (course.getStatus() != CourseStatus.DELEDED
                && course.getStatus() != CourseStatus.BANNED) {
            course.setStatus(CourseStatus.DRAFT);
        }
    }

    private void deleteLessonFileAfterCommit(String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        awsS3Service.deleteFileFromS3(objectKey);
                    }
                });
    }
}
