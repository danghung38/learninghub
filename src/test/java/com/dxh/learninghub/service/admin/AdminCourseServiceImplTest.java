package com.dxh.learninghub.service.admin;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.service.impl.admin.AdminCourseServiceImpl;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceImplTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseMapper courseMapper;
    @Mock NotificationService notificationService;
    @Mock CurrentUserProvider currentUser;

    @InjectMocks AdminCourseServiceImpl adminCourseService;

    @Test
    void approve_pendingCourse_changesStatusAndNotifiesAuthor() {
        User author = user(2L);
        User admin = user(1L);
        Course course = course(10L, author, CourseStatus.PENDING);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(currentUser.getCurrentUser()).thenReturn(admin);

        adminCourseService.approve(10L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.APPROVED);
        verify(notificationService).createNotification(
                eq(author), eq(admin), eq("Course approved"),
                contains("has been approved"), eq("/teacher/courses/10/preview"));
    }

    @Test
    void reject_blankReason_usesSafeDefaultReason() {
        User author = user(2L);
        User admin = user(1L);
        Course course = course(10L, author, CourseStatus.PENDING);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(currentUser.getCurrentUser()).thenReturn(admin);

        adminCourseService.reject(10L, "  ");

        assertThat(course.getStatus()).isEqualTo(CourseStatus.REJECTED);
        verify(notificationService).createNotification(
                eq(author), eq(admin), eq("Course rejected"),
                contains("No specific reason provided."), eq("/teacher/courses/10/preview"));
    }

    @Test
    void approve_nonPendingCourse_isRejectedWithoutNotification() {
        Course course = course(10L, user(2L), CourseStatus.DRAFT);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> adminCourseService.approve(10L))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_PENDING));
        verify(notificationService, never()).createNotification(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unban_bannedCourse_returnsItToDraft() {
        Course course = course(10L, user(2L), CourseStatus.BANNED);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(currentUser.getCurrentUser()).thenReturn(user(1L));

        adminCourseService.unban(10L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    private static User user(Long id) {
        User user = User.builder().username("user" + id).email("u" + id + "@example.com").build();
        user.setId(id);
        return user;
    }

    private static Course course(Long id, User author, CourseStatus status) {
        Course course = Course.builder().title("Java Backend").author(author).status(status).build();
        course.setId(id);
        return course;
    }
}
