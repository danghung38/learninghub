package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.request.LessonRequest;
import com.dxh.learninghub.dto.request.LessonReorderRequest;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Chapter chapter = getChapterAndValidateOwnerForUpdate(request.chapterId());
        markAsDraft(chapter.getCourse());

        Lesson lesson = lessonMapper.lessonRequestToLesson(request);
        lesson.setChapter(chapter);
        lesson.setContentUrl(awsS3Service.normalizeObjectKey(request.contentUrl()));

        List<Lesson> orderedLessons = getOrderedLessons(chapter);
        int desiredOrder = normalizeOrder(request.lessonOrder(), orderedLessons.size() + 1);
        orderedLessons.add(desiredOrder - 1, lesson);
        renumberLessons(orderedLessons);

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
        Long sourceChapterId = lesson.getChapter().getId();
        Long targetChapterId = request.chapterId() == null ? sourceChapterId : request.chapterId();
        Map<Long, Chapter> lockedChapters = lockChaptersAndValidateOwner(sourceChapterId, targetChapterId);
        Chapter sourceChapter = lockedChapters.get(sourceChapterId);
        Chapter targetChapter = lockedChapters.get(targetChapterId);

        if (!Objects.equals(sourceChapter.getCourse().getId(), targetChapter.getCourse().getId())) {
            throw new AppException(ErrorCode.LESSON_CROSS_COURSE_MOVE_NOT_ALLOWED);
        }
        markAsDraft(targetChapter.getCourse());

        String oldObjectKey = awsS3Service.normalizeObjectKey(lesson.getContentUrl());
        lessonMapper.updateLessonFromRequest(request, lesson);

        List<Lesson> sourceLessons = getOrderedLessons(sourceChapter);
        int currentOrder = findLessonOrder(sourceLessons, lesson.getId());
        sourceLessons.removeIf(item -> Objects.equals(item.getId(), lesson.getId()));

        if (Objects.equals(sourceChapterId, targetChapterId)) {
            int desiredOrder = request.lessonOrder() == null
                    ? normalizeOrder(currentOrder, sourceLessons.size() + 1)
                    : normalizeOrder(request.lessonOrder(), sourceLessons.size() + 1);
            sourceLessons.add(desiredOrder - 1, lesson);
            renumberLessons(sourceLessons);
        } else {
            renumberLessons(sourceLessons);
            List<Lesson> targetLessons = getOrderedLessons(targetChapter);
            int desiredOrder = normalizeOrder(request.lessonOrder(), targetLessons.size() + 1);
            lesson.setChapter(targetChapter);
            targetLessons.add(desiredOrder - 1, lesson);
            renumberLessons(targetLessons);
        }

        lessonRepository.saveAndFlush(lesson);

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
    public List<LessonResponse> reorderLessons(LessonReorderRequest request) {
        Chapter chapter = getChapterAndValidateOwnerForUpdate(request.chapterId());
        List<Lesson> currentLessons = getOrderedLessons(chapter);
        List<Long> requestedIds = request.lessonIds();

        if (requestedIds.size() != currentLessons.size()
                || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<Long, Lesson> lessonsById = new LinkedHashMap<>();
        currentLessons.forEach(lesson -> lessonsById.put(lesson.getId(), lesson));

        List<Lesson> reorderedLessons = new ArrayList<>(requestedIds.size());
        for (Long lessonId : requestedIds) {
            Lesson lesson = lessonsById.get(lessonId);
            if (lesson == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            reorderedLessons.add(lesson);
        }

        renumberLessons(reorderedLessons);
        markAsDraft(chapter.getCourse());
        lessonRepository.flush();

        return reorderedLessons.stream()
                .map(lessonMapper::lessonToLessonResponse)
                .toList();
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
        Chapter chapter = getChapterAndValidateOwnerForUpdate(lesson.getChapter().getId());
        Long courseId = chapter.getCourse().getId();
        markAsDraft(chapter.getCourse());

        String objectKey = lesson.getContentUrl();
        lessonProgressRepository.deleteAllByLessonId(lesson.getId());
        lessonRepository.delete(lesson);
        List<Lesson> remainingLessons = getOrderedLessons(chapter);
        remainingLessons.removeIf(item -> Objects.equals(item.getId(), lesson.getId()));
        renumberLessons(remainingLessons);
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

        return lessonRepository.findByChapterOrderByLessonOrderAscIdAsc(chapter).stream()
                .map(lessonMapper::lessonToLessonResponse)
                .toList();
    }

    private Lesson getLessonOrThrow(Long id) {
        return lessonRepository.findWithCourseById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
    }

    private Chapter getChapterAndValidateOwnerForUpdate(Long chapterId) {
        Chapter chapter = chapterRepository.findWithCourseAndAuthorByIdForUpdate(chapterId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_EXISTED));
        validateOwner(chapter);
        return chapter;
    }

    private Map<Long, Chapter> lockChaptersAndValidateOwner(Long firstChapterId, Long secondChapterId) {
        List<Long> chapterIds = List.of(firstChapterId, secondChapterId).stream()
                .distinct()
                .sorted()
                .toList();
        Map<Long, Chapter> chapters = new LinkedHashMap<>();
        chapterIds.forEach(chapterId -> {
            Chapter chapter = getChapterAndValidateOwnerForUpdate(chapterId);
            chapters.put(chapterId, chapter);
        });
        return chapters;
    }

    private List<Lesson> getOrderedLessons(Chapter chapter) {
        return new ArrayList<>(lessonRepository.findByChapterOrderByLessonOrderAscIdAsc(chapter));
    }

    private int normalizeOrder(Integer requestedOrder, int maximumOrder) {
        if (requestedOrder == null) {
            return maximumOrder;
        }
        return Math.max(1, Math.min(requestedOrder, maximumOrder));
    }

    private int findLessonOrder(List<Lesson> lessons, Long lessonId) {
        for (int index = 0; index < lessons.size(); index++) {
            if (Objects.equals(lessons.get(index).getId(), lessonId)) {
                return index + 1;
            }
        }
        throw new AppException(ErrorCode.LESSON_NOT_EXISTED);
    }

    private void renumberLessons(List<Lesson> lessons) {
        for (int index = 0; index < lessons.size(); index++) {
            lessons.get(index).setLessonOrder(index + 1);
        }
    }

    private void validateOwner(Chapter chapter) {
        User currentUser = currentUserProvider.getCurrentUser();
        User author = chapter.getCourse().getAuthor();

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));

        if (!isAdmin && !Objects.equals(currentUser.getId(), author.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!isAdmin && chapter.getCourse().getStatus() == CourseStatus.DELETED) {
            throw new AppException(ErrorCode.COURSE_DELETED_READ_ONLY);
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
        if (course.getStatus() != CourseStatus.DELETED
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
