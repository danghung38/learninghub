package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.specification.CourseSpecification;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.interfac.admin.AdminCourseService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCourseServiceImpl implements AdminCourseService {

    CourseRepository courseRepository;
    CourseMapper courseMapper;
    NotificationService notificationService;
    CurrentUserProvider currentUser;

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<CourseResponse> searchCourses(Pageable pageable, CourseSearchFilterRequest filter) {
        Specification<Course> spec = CourseSpecification.adminSearch(filter);
        return buildPageResponse(spec, pageable);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CourseResponse approve(Long id) {
        Course course = findCourse(id);
        validatePending(course);
        course.setStatus(CourseStatus.APPROVED);
        notifyCourseAuthor(course, "Course approved",
                "Your course \"" + course.getTitle() + "\" has been approved");
        return courseMapper.courseToCourseResponse(course);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void ban(Long id) {
        Course course = findCourse(id);
        course.setStatus(CourseStatus.BANNED);
        notifyCourseAuthor(course, "Course banned", "Your course \"" + course.getTitle() + "\" has been banned");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void unban(Long id) {
        Course course = findCourse(id);
        if (course.getStatus() != CourseStatus.BANNED) {
            throw new AppException(ErrorCode.COURSE_NOT_BANNED);
        }

        course.setStatus(CourseStatus.DRAFT);
        notifyCourseAuthor(course, "Course unbanned", "Your course \"" + course.getTitle() + "\" has been unbanned and returned to draft for review and resubmission");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COURSES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COURSE_TITLE, allEntries = true)
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CourseResponse reject(Long id) {
        Course course = findCourse(id);
        validatePending(course);
        course.setStatus(CourseStatus.REJECTED);
        notifyCourseAuthor(course, "Course rejected", "Your course \"" + course.getTitle() + "\" has been rejected");
        return courseMapper.courseToCourseResponse(course);
    }

    private void notifyCourseAuthor(Course course, String title, String message) {
        notificationService.createNotification(
                course.getAuthor(),
                currentUser.getCurrentUser(),
                title,
                message,
                "/teacher/courses/" + course.getId() + "/preview");
    }

    private void validatePending(Course course) {
        if (course.getStatus() != CourseStatus.PENDING) {
            throw new AppException(ErrorCode.COURSE_NOT_PENDING);
        }
    }

    private Course findCourse(Long id) {
        return courseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
    }

    private PageResponse<CourseResponse> buildPageResponse(Specification<Course> spec, Pageable pageable) {
        Page<Course> page = courseRepository.findAll(spec, pageable);

        return PageResponse.<CourseResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(courseMapper::courseToCourseResponse).toList())
                .build();
    }
}