package com.dxh.learninghub.service.impl;

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
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    CourseMapper courseMapper;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    CourseRepository courseRepository;

    @Mock
    LessonRepository lessonRepository;

    @Mock
    AwsS3Service awsS3Service;

    @Mock
    NotificationService notificationService;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CourseServiceImpl courseService;

    @Test
    void submitCourse_movesDraftToPendingAndNotifiesAdminOnce() {
        User teacher = user(10L, "Teacher");
        User admin = user(1L, "Admin");
        Course course = course(25L, teacher, CourseStatus.DRAFT);

        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));
        when(lessonRepository.countByChapterCourseId(25L)).thenReturn(1L);
        when(courseRepository.save(course)).thenReturn(course);
        when(userRepository.findFirstByRoles_Name(RoleEnum.ADMIN.name()))
                .thenReturn(Optional.of(admin));

        courseService.submitCourse(25L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING);
        verify(notificationService).createNotification(
                admin,
                teacher,
                "New course pending approval",
                "Teacher submitted the completed course \"Spring Boot\" for approval",
                "/admin/courses/25");
    }

    @Test
    void submitCourse_rejectsNonDraftCourseWithoutAnotherNotification() {
        User teacher = user(10L, "Teacher");
        Course course = course(25L, teacher, CourseStatus.PENDING);

        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.submitCourse(25L))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_DRAFT);

        verify(courseRepository, never()).save(any(Course.class));
        verify(notificationService, never()).createNotification(
                any(), any(), any(), any(), any());
    }

    @Test
    void submitCourse_rejectsDraftWithoutAnyLesson() {
        User teacher = user(10L, "Teacher");
        Course course = course(25L, teacher, CourseStatus.DRAFT);

        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));
        when(lessonRepository.countByChapterCourseId(25L)).thenReturn(0L);

        assertThatThrownBy(() -> courseService.submitCourse(25L))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_CONTENT_INCOMPLETE);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void softDeleteCourse_marksOwnedCourseAsDeleded() {
        User teacher = user(10L, "Teacher");
        Course course = course(25L, teacher, CourseStatus.APPROVED);

        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findWithAuthorById(25L)).thenReturn(Optional.of(course));

        courseService.softDeleteCourse(25L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DELEDED);
    }

    private User user(Long id, String fullName) {
        User user = User.builder()
                .username(fullName.toLowerCase())
                .email(fullName.toLowerCase() + "@example.com")
                .fullName(fullName)
                .build();
        user.setId(id);
        return user;
    }

    private Course course(Long id, User author, CourseStatus status) {
        Course course = Course.builder()
                .title("Spring Boot")
                .author(author)
                .status(status)
                .build();
        course.setId(id);
        return course;
    }
}
