package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.service.interfac.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceImplTest {

    @Mock
    CourseRepository courseRepository;

    @Mock
    CourseMapper courseMapper;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    AdminCourseServiceImpl adminCourseService;

    @Test
    void approve_updatesStatusAndNotifiesTeacher() {
        Course course = submittedCourse();
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));

        adminCourseService.approve(25L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.APPROVED);
        verify(notificationService).createNotification(
                course.getAuthor(),
                null,
                "Course approved",
                "Your course \"Spring Boot\" has been approved",
                "/teacher/courses/25/preview");
    }

    @Test
    void reject_updatesStatusAndNotifiesTeacher() {
        Course course = submittedCourse();
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));

        adminCourseService.reject(25L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.REJECTED);
        verify(notificationService).createNotification(
                course.getAuthor(),
                null,
                "Course rejected",
                "Your course \"Spring Boot\" has been rejected",
                "/teacher/courses/25/preview");
    }

    @Test
    void approve_rejectsCourseThatIsNotPending() {
        Course course = submittedCourse();
        course.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> adminCourseService.approve(25L))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_PENDING);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        verify(notificationService, never()).createNotification(
                course.getAuthor(), null, "Course approved",
                "Your course \"Spring Boot\" has been approved",
                "/teacher/courses/25/preview");
    }

    private Course submittedCourse() {
        User teacher = User.builder()
                .username("teacher")
                .email("teacher@example.com")
                .fullName("Teacher")
                .build();
        teacher.setId(10L);

        Course course = Course.builder()
                .title("Spring Boot")
                .author(teacher)
                .status(CourseStatus.PENDING)
                .build();
        course.setId(25L);
        return course;
    }
}
