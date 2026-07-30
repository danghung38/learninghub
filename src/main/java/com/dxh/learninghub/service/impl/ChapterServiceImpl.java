package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.request.ChapterRequest;
import com.dxh.learninghub.dto.request.ChapterUpdateRequest;
import com.dxh.learninghub.dto.response.ChapterResponse;
import com.dxh.learninghub.entity.Chapter;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.ChapterMapper;
import com.dxh.learninghub.repo.ChapterRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.LessonProgressRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.ChapterService;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChapterServiceImpl implements ChapterService {
    ChapterRepository chapterRepository;
    CourseRepository courseRepository;
    LearningProgressService learningProgressService;
    LessonProgressRepository lessonProgressRepository;
    ChapterMapper chapterMapper;
    CurrentUserProvider currentUserProvider;
    AwsS3Service awsS3Service;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public ChapterResponse create(ChapterRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        validateCanManage(course, ErrorCode.UNAUTHORIZED);
        markAsDraft(course);

        Chapter chapter = chapterMapper.toChapter(request);
        chapter.setCourse(course);
        return chapterMapper.toChapterResponse(chapterRepository.save(chapter));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public ChapterResponse update(Long id, ChapterUpdateRequest request) {
        Chapter chapter = findChapter(id);
        validateCanManage(chapter.getCourse(), ErrorCode.NOT_COURSE_OWNER);
        markAsDraft(chapter.getCourse());
        chapterMapper.updateFromRequest(request, chapter);
        return chapterMapper.toChapterResponse(chapterRepository.save(chapter));
    }
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public void delete(Long id) {
        Chapter chapter = findChapter(id);
        validateCanManage(chapter.getCourse(), ErrorCode.NOT_CHAPTER_OWNER);
        markAsDraft(chapter.getCourse());

        List<String> lessonObjectKeys = chapter.getLessons().stream()
                .map(Lesson::getContentUrl)
                .filter(contentUrl -> contentUrl != null && !contentUrl.isBlank())
                .toList();
        Long courseId = chapter.getCourse().getId();

        lessonProgressRepository.deleteAllByChapterId(chapter.getId());
        chapterRepository.delete(chapter);
        chapterRepository.flush();
        learningProgressService.synchronizeCourse(courseId);
        deleteLessonFilesAfterCommit(lessonObjectKeys);
    }

    private void deleteLessonFilesAfterCommit(List<String> objectKeys) {
        if (objectKeys.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        objectKeys.forEach(awsS3Service::deleteFileFromS3);
                    }
                });
    }

    private Chapter findChapter(Long id) {
        return chapterRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_EXISTED));
    }

    private void validateCanManage(Course course, ErrorCode ownershipError) {
        User user = currentUserProvider.getCurrentUser();
        boolean admin = user.getRoles().stream().anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));

        if (!admin && !course.getAuthor().getId().equals(user.getId())) {
            throw new AppException(ownershipError);
        }
        if (!admin && course.getStatus() == CourseStatus.DELEDED) {
            throw new AppException(ErrorCode.COURSE_DELEDED_READ_ONLY);
        }
    }

    private void markAsDraft(Course course) {
        if (course.getStatus() != CourseStatus.DELEDED
                && course.getStatus() != CourseStatus.BANNED) {
            course.setStatus(CourseStatus.DRAFT);
        }
    }


}
