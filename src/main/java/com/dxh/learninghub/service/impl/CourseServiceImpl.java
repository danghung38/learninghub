package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.request.CourseUploadRequest;
import com.dxh.learninghub.dto.request.CourseUpdateRequest;
import com.dxh.learninghub.dto.response.CoursePreviewResponse;
import com.dxh.learninghub.dto.response.CourseManagementPreviewResponse;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.CourseUploadResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.LessonRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.repo.specification.CourseSpecification;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.CourseService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseMapper courseMapper;
    CurrentUserProvider currentUserProvider;
    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    AwsS3Service awsS3Service;
    NotificationService notificationService;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.COURSES, key = "#courseId", sync = true)
    public CourseResponse getCourse(Long courseId) {
        Course course = courseRepository.findPublicCourseById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_AVAILABLE));
        return courseMapper.courseToCourseResponse(course);
    }

    @Cacheable(
            cacheNames = CacheNames.COURSE_TITLE,
            key = "#query.trim().toLowerCase()",
            sync = true)
    public List<String> getTitleSuggestions(String query) {
        return courseRepository.findTitleSuggestions(query);
    }


    @Override
    @Transactional(readOnly = true)
    public CoursePreviewResponse getCoursePreview(Long courseId) {
        Course course = courseRepository.findWithChaptersAndLessonsById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        return courseMapper.courseToCoursePreviewResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public CourseManagementPreviewResponse getManagementPreview(Long courseId) {
        Course course = courseRepository.findWithChaptersAndLessonsById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        User currentUser = currentUserProvider.getCurrentUser();
        if (course.getStatus() == CourseStatus.DELETED && !currentUser.isAdmin()) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }
        validateAdminOrCourseOwner(course, currentUser);

        return courseMapper.courseToManagementPreviewResponse(course);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public List<CourseResponse> getMyCourses() {
        User user = currentUserProvider.getCurrentUser();
        return courseRepository.findByAuthorAndStatusNot(user, CourseStatus.DELETED)
                .stream()
                .map(courseMapper::courseToCourseResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public CourseUploadResponse createCourse(CourseUploadRequest request, MultipartFile thumbnail) {
        FileUploadUtil.validate(thumbnail, UploadPolicy.IMAGE);

        User teacher = currentUserProvider.getCurrentUser();

        Course course = courseMapper.courseUploadToCourse(request);
        course.setThumbnail(awsS3Service.uploadFile(thumbnail,
                "courses/" + teacher.getId() + "/thumbnails",
                UploadPolicy.IMAGE));
        course.setAuthor(teacher);
        course.setStatus(CourseStatus.DRAFT);

        Course savedCourse = courseRepository.save(course);
        return courseMapper.courseToCourseUploadResponse(savedCourse);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#courseId"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    public CourseUploadResponse updateCourse(Long courseId, CourseUpdateRequest request, MultipartFile thumbnail) {
        FileUploadUtil.validateIfPresent(thumbnail, UploadPolicy.IMAGE);
        Course course = getManagedCourse(courseId);
        if (course.getStatus() == CourseStatus.DELETED
                && !currentUserProvider.getCurrentUser().isAdmin()) {
            throw new AppException(ErrorCode.COURSE_DELETED_READ_ONLY);
        }

        CourseStatus oldStatus = course.getStatus();
        String oldThumbnail = course.getThumbnail();

        courseMapper.updateCourseFromRequest(request, course);

        boolean hasNewThumbnail = thumbnail != null && !thumbnail.isEmpty();

        if (hasNewThumbnail) {
            course.setThumbnail(awsS3Service.uploadFile(thumbnail,
                    "courses/" + course.getAuthor().getId() + "/thumbnails",
                    UploadPolicy.IMAGE));
        }

        boolean editableLifecycle = oldStatus != CourseStatus.DELETED
                && oldStatus != CourseStatus.BANNED;
        course.setStatus(editableLifecycle ? CourseStatus.DRAFT : oldStatus);

        Course savedCourse = courseRepository.save(course);

        if (hasNewThumbnail && oldThumbnail != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    awsS3Service.deleteFileFromS3(oldThumbnail);
                }
            });
        }

        return courseMapper.courseToCourseUploadResponse(savedCourse);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#courseId"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    public CourseUploadResponse submitCourse(Long courseId) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        validateAdminOrCourseOwner(course, currentUserProvider.getCurrentUser());

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new AppException(ErrorCode.COURSE_NOT_DRAFT);
        }
        if (lessonRepository.countByChapterCourseId(courseId) == 0) {
            throw new AppException(ErrorCode.COURSE_CONTENT_INCOMPLETE);
        }

        course.setStatus(CourseStatus.PENDING);
        Course savedCourse = courseRepository.save(course);

        notifyAdmin(
                savedCourse.getAuthor(),
                "New course pending approval",
                savedCourse.getAuthor().getFullName()
                        + " submitted the completed course \""
                        + savedCourse.getTitle() + "\" for approval",
                "/admin/courses/" + savedCourse.getId());

        return courseMapper.courseToCourseUploadResponse(savedCourse);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#courseId"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    public void softDeleteCourse(Long courseId) {
        Course course = getManagedCourse(courseId);
        User currentUser = currentUserProvider.getCurrentUser();

        if (course.getStatus() == CourseStatus.DELETED) {
            throw new AppException(ErrorCode.COURSE_ALREADY_DELEDED);
        }
        if (course.getStatus() == CourseStatus.BANNED && !currentUser.isAdmin()) {
            throw new AppException(ErrorCode.COURSE_BANNED_CANNOT_DELETE);
        }
        course.setStatus(CourseStatus.DELETED);
    }

    @Override
    @Cacheable(
            cacheNames = CacheNames.COURSE_LIST,
            keyGenerator = "courseListKeyGenerator",
            sync = true)
    public PageResponse<CourseResponse> searchCourses(Pageable pageable, CourseSearchFilterRequest filter) {
        Page<Course> courses = courseRepository.findAll(
                CourseSpecification.publicSearch(filter),
                pageable);

        return PageResponse.<CourseResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(courses.getTotalPages())
                .totalElements(courses.getTotalElements())
                .items(courses.stream()
                        .map(courseMapper::courseToCourseResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCoursesByTeacher(Long teacherId, Pageable pageable) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean isTeacher = teacher.getRoles().stream()
                .anyMatch(role -> RoleEnum.TEACHER.name().equals(role.getName()));
        if (!isTeacher) {
            throw new AppException(ErrorCode.USER_NOT_TEACHER);
        }

        Page<Course> courses = courseRepository.findByAuthorIdAndStatus(
                teacherId,
                CourseStatus.APPROVED,
                pageable);

        return PageResponse.<CourseResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(courses.getTotalPages())
                .totalElements(courses.getTotalElements())
                .items(courses.stream().map(courseMapper::courseToCourseResponse).toList())
                .build();
    }

    private Course getManagedCourse(Long courseId) {
        Course course = courseRepository.findWithAuthorById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        validateAdminOrCourseOwner(course, currentUserProvider.getCurrentUser());
        return course;
    }

    private void validateAdminOrCourseOwner(Course course, User currentUser) {
        boolean isCourseOwner = course.getAuthor() != null
                && Objects.equals(course.getAuthor().getId(), currentUser.getId());
        if (!currentUser.isAdmin() && !isCourseOwner) throw new AppException(ErrorCode.NOT_COURSE_OWNER);

    }


    private void notifyAdmin(User sender, String title, String message, String url) {
        userRepository.findFirstByRoles_Name(RoleEnum.ADMIN.name())
                .ifPresent(admin -> notificationService.createNotification(admin, sender, title, message, url));
    }
}
