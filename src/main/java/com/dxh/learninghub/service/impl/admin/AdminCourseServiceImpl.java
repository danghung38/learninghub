package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.constant.CacheNames;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.specification.CourseSpecifications;
import com.dxh.learninghub.repo.specification.GenericSpecificationBuilder;
import com.dxh.learninghub.repo.specification.SpecSearchCriteria;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.interfac.admin.AdminCourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dxh.learninghub.constant.AppConstant.SEARCH_SPEC_OPERATOR;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminCourseServiceImpl implements AdminCourseService {

    static Pattern SPEC_PATTERN = Pattern.compile(SEARCH_SPEC_OPERATOR);
    CourseRepository courseRepository;
    CourseMapper courseMapper;
    NotificationService notificationService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<CourseResponse> searchCourses(Pageable pageable, String[] course, String[] author) {

        Specification<Course> spec = Specification.where(buildCourseSpecification(course)).and(buildAuthorSpec(author));

        return buildPageResponse(spec, pageable);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<CourseResponse> getByStatus(CourseStatus status, Pageable pageable) {

        Specification<Course> spec = CourseSpecifications.hasStatus(status);

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
        notifyCourseAuthor(course, "Course approved", "Your course \"" + course.getTitle() + "\" has been approved");
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
                null,
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

        return PageResponse.<CourseResponse>builder().pageNo(pageable.getPageNumber() + 1).pageSize(pageable.getPageSize()).totalPage(page.getTotalPages()).totalElements(page.getTotalElements()).items(page.stream().map(courseMapper::courseToCourseResponse).toList()).build();
    }

    private Specification<Course> buildCourseSpecification(String[] conditions) {

        if (conditions == null || conditions.length == 0) {
            return Specification.where(null);
        }

        GenericSpecificationBuilder<Course> builder = new GenericSpecificationBuilder<>();

        Arrays.stream(conditions).map(SPEC_PATTERN::matcher).filter(Matcher::find).map(m -> new SpecSearchCriteria(null, m.group(1), m.group(2), m.group(3), m.group(4), m.group(5))).forEach(builder::with);

        return builder.build();
    }

    private Specification<Course> buildAuthorSpec(String[] authors) {

        if (authors == null || authors.length == 0) {
            return Specification.where(null);
        }

        return Arrays.stream(authors).map(SPEC_PATTERN::matcher).filter(Matcher::find).map(m -> CourseSpecifications.hasAuthor(m.group(4))).reduce(Specification.where(null), Specification::and);
    }
}
