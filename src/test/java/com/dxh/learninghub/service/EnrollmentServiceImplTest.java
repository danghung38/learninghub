package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.request.BuyCourseRequest;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Enrollment;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.CourseProgressRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.EnrollmentServiceImpl;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock CurrentUserProvider currentUserProvider;
    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock PointTransactionRepository pointTransactionRepository;
    @Mock CourseProgressRepository courseProgressRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks EnrollmentServiceImpl enrollmentService;

    @Test
    void buyCourse_transfersPointsAndCreatesLearningDataAtomically() {
        User buyer = user(1L, 500L, "Học viên");
        User teacher = user(2L, 100L, "Giảng viên");
        Course course = course(10L, teacher, 200L, CourseStatus.APPROVED);

        when(currentUserProvider.getCurrentUser()).thenReturn(buyer);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(userRepository.findAllByIdForUpdate(List.of(1L, 2L)))
                .thenReturn(List.of(buyer, teacher));
        when(enrollmentRepository.existsByUserAndCourse(buyer, course)).thenReturn(false);

        enrollmentService.buyCourse(BuyCourseRequest.builder().courseId(10L).build());

        assertThat(buyer.getPoints()).isEqualTo(300L);
        assertThat(teacher.getPoints()).isEqualTo(300L);
        assertThat(course.getTotalEnrollments()).isEqualTo(1L);
        verify(pointTransactionRepository).saveAll(any());
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(courseProgressRepository).save(any());
        verify(notificationService).createNotification(
                teacher, buyer, "New course enrollment",
                "Học viên enrolled in \"Java Backend\"",
                "/teacher/revenue/courses/10/students");
    }

    @Test
    void buyCourse_whenBalanceIsInsufficient_keepsBalancesUnchanged() {
        User buyer = user(1L, 50L, "Học viên");
        User teacher = user(2L, 100L, "Giảng viên");
        Course course = course(10L, teacher, 200L, CourseStatus.APPROVED);
        when(currentUserProvider.getCurrentUser()).thenReturn(buyer);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));
        when(userRepository.findAllByIdForUpdate(List.of(1L, 2L)))
                .thenReturn(List.of(buyer, teacher));
        when(enrollmentRepository.existsByUserAndCourse(buyer, course)).thenReturn(false);

        assertError(
                () -> enrollmentService.buyCourse(new BuyCourseRequest(10L)),
                ErrorCode.BUY_COURSE_INVALID);

        assertThat(buyer.getPoints()).isEqualTo(50L);
        assertThat(teacher.getPoints()).isEqualTo(100L);
        verify(pointTransactionRepository, never()).saveAll(any());
    }

    @Test
    void buyCourse_whenBuyerIsAuthor_isRejectedBeforeLockingBalances() {
        User author = user(1L, 500L, "Tác giả");
        Course course = course(10L, author, 200L, CourseStatus.APPROVED);
        when(currentUserProvider.getCurrentUser()).thenReturn(author);
        when(courseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(course));

        assertError(
                () -> enrollmentService.buyCourse(new BuyCourseRequest(10L)),
                ErrorCode.CANNOT_BUY_OWN_COURSE);

        verify(userRepository, never()).findAllByIdForUpdate(any());
    }

    @Test
    void getEnrollmentStatus_withoutEnrollment_returnsNotEnrolled() {
        User user = user(1L, 0L, "Học viên");
        Course course = course(10L, user(2L, 0L, "Giảng viên"), 100L, CourseStatus.APPROVED);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserAndCourse(user, course)).thenReturn(Optional.empty());

        var result = enrollmentService.getEnrollmentStatus(10L);

        assertThat(result.enrolled()).isFalse();
        assertThat(result.status()).isNull();
    }

    private static User user(Long id, Long points, String fullName) {
        User user = User.builder().username("user" + id).email("u" + id + "@example.com")
                .fullName(fullName).points(points).build();
        user.setId(id);
        return user;
    }

    private static Course course(Long id, User author, Long points, CourseStatus status) {
        Course course = Course.builder().title("Java Backend").author(author).points(points)
                .status(status).totalEnrollments(0L).build();
        course.setId(id);
        return course;
    }

    private static void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
    }
}
