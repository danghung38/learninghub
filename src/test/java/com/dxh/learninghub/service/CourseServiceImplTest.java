package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.CourseUploadResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.CourseMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.LessonRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.CourseServiceImpl;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock CourseMapper courseMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;
    @InjectMocks CourseServiceImpl service;

    @Test
    void getCourse_mapsPublicCourse() {
        Course course = course(1L, CourseStatus.APPROVED, teacher(3L));
        CourseResponse response = CourseResponse.builder().id(1L).build();
        when(courseRepository.findPublicCourseById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.courseToCourseResponse(course)).thenReturn(response);

        assertThat(service.getCourse(1L)).isSameAs(response);
    }

    @Test
    void getCoursePreview_rejectsNonApprovedCourse() {
        when(courseRepository.findWithChaptersAndLessonsById(1L))
                .thenReturn(Optional.of(course(1L, CourseStatus.PENDING, teacher(3L))));

        assertError(() -> service.getCoursePreview(1L), ErrorCode.COURSE_NOT_AVAILABLE);
        verify(courseMapper, never()).courseToCoursePreviewResponse(any());
    }

    @Test
    void submitCourse_movesDraftToPendingAndNotifiesAdmin() {
        User owner = teacher(3L);
        User admin = User.builder().fullName("Admin").build();
        admin.setId(1L);
        Course course = course(8L, CourseStatus.DRAFT, owner);
        CourseUploadResponse response = CourseUploadResponse.builder().id(8L).build();
        when(courseRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(course));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(lessonRepository.countByChapterCourseId(8L)).thenReturn(2L);
        when(courseRepository.save(course)).thenReturn(course);
        when(userRepository.findFirstByRoles_Name(RoleEnum.ADMIN.name())).thenReturn(Optional.of(admin));
        when(courseMapper.courseToCourseUploadResponse(course)).thenReturn(response);

        assertThat(service.submitCourse(8L)).isSameAs(response);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING);
        verify(notificationService).createNotification(eq(admin), eq(owner), anyString(), anyString(), eq("/admin/courses/8"));
    }

    @Test
    void submitCourse_rejectsEmptyContent() {
        User owner = teacher(3L);
        Course course = course(8L, CourseStatus.DRAFT, owner);
        when(courseRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(course));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(lessonRepository.countByChapterCourseId(8L)).thenReturn(0L);

        assertError(() -> service.submitCourse(8L), ErrorCode.COURSE_CONTENT_INCOMPLETE);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void softDeleteCourse_rejectsNonOwner() {
        Course course = course(8L, CourseStatus.APPROVED, teacher(3L));
        when(courseRepository.findWithAuthorById(8L)).thenReturn(Optional.of(course));
        when(currentUserProvider.getCurrentUser()).thenReturn(teacher(9L));

        assertError(() -> service.softDeleteCourse(8L), ErrorCode.NOT_COURSE_OWNER);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.APPROVED);
    }

    private static Course course(Long id, CourseStatus status, User owner) {
        Course course = Course.builder().title("Java").status(status).author(owner).build();
        course.setId(id);
        return course;
    }

    private static User teacher(Long id) {
        User user = User.builder().fullName("Teacher").roles(Set.of(Role.builder().name(RoleEnum.TEACHER.name()).build())).build();
        user.setId(id);
        return user;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
