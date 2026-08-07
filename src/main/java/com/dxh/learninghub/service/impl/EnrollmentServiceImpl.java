package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.dto.request.BuyCourseRequest;
import com.dxh.learninghub.dto.response.BuyCourseResponse;
import com.dxh.learninghub.dto.response.EnrollmentStatusResponse;
import com.dxh.learninghub.dto.response.MyCourseResponse;
import com.dxh.learninghub.entity.*;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.EnrollmentMapper;
import com.dxh.learninghub.repo.*;
import com.dxh.learninghub.service.interfac.EnrollmentService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    CurrentUserProvider currentUserProvider;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    PointTransactionRepository pointTransactionRepository;
    EnrollmentMapper enrollmentMapper;
    CourseProgressRepository courseProgressRepository;
    NotificationService notificationService;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseResponse> getCourseByUserCurrent(){
        User user = currentUserProvider.getCurrentUser();

        List<Enrollment> enrollments = enrollmentRepository.findCourseByUser(user);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        return enrollments.stream()
                .map(enrollmentMapper::toMyCourseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatusResponse getEnrollmentStatus(Long courseId) {
        User user = currentUserProvider.getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        Enrollment enrollment = enrollmentRepository.findByUserAndCourse(user, course)
                .orElse(null);

        if (enrollment == null) {
            return EnrollmentStatusResponse.builder()
                    .courseId(courseId)
                    .enrolled(false)
                    .build();
        }

        boolean enrolled = enrollment.getStatus() == EnrollmentStatus.ACTIVE
                || enrollment.getStatus() == EnrollmentStatus.COMPLETED;

        return EnrollmentStatusResponse.builder()
                .courseId(courseId)
                .enrolled(enrolled)
                .status(enrollment.getStatus())
                .build();
    }


    @Transactional
    @Override
    @PreAuthorize("isAuthenticated()")
    public BuyCourseResponse buyCourse(BuyCourseRequest request) {

        User authenticatedUser = currentUserProvider.getCurrentUser();

        Course course = courseRepository.findByIdForUpdate(request.courseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        Long buyerId = authenticatedUser.getId();
        Long teacherId = course.getAuthor().getId();
        if (Objects.equals(teacherId, buyerId)) {
            throw new AppException(ErrorCode.CANNOT_BUY_OWN_COURSE);
        }

        Map<Long, User> lockedUsers = userRepository
                .findAllByIdForUpdate(List.of(buyerId, teacherId))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        User user = lockedUsers.get(buyerId);
        User teacher = lockedUsers.get(teacherId);
        if (user == null || teacher == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        if (enrollmentRepository.existsByUserAndCourse(user, course)) {
            throw new AppException(ErrorCode.COURSE_ALREADY_PURCHASED);
        }

        long userPoints = user.getPoints() == null ? 0L : user.getPoints();
        Long coursePoints = course.getPoints();

        if (userPoints < coursePoints) {
            throw new AppException(ErrorCode.BUY_COURSE_INVALID);
        }

        // Trừ điểm
        user.setPoints(Math.subtractExact(userPoints, coursePoints));

        //cộng điểm
        long teacherPoints = teacher.getPoints() == null ? 0L : teacher.getPoints();
        teacher.setPoints(Math.addExact(teacherPoints, coursePoints));

        // Tăng lượt mua
        long totalEnrollments = course.getTotalEnrollments() == null
                ? 0L
                : course.getTotalEnrollments();
        course.setTotalEnrollments(Math.addExact(totalEnrollments, 1L));

        // Lưu lịch sử điểm
        PointTransaction studentTransaction = PointTransaction.builder()
                .user(user)
                .course(course)
                .payment(null)
                .points(coursePoints)
                .transactionType(PointTransactionType.SPEND)
                .description("Purchase course: " + course.getTitle())
                .build();
        //k cần lưu tự commit vì cùng Transactional

        PointTransaction teacherTransaction =
                PointTransaction.builder()
                        .user(teacher)
                        .course(course)
                        .payment(null)
                        .points(coursePoints)
                        .transactionType(PointTransactionType.EARN)
                        .description("Income from selling course: " + course.getTitle())
                        .build();

        pointTransactionRepository.saveAll(
                List.of(studentTransaction, teacherTransaction)
        );

        // Cấp quyền học
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .spentPoints(coursePoints)
                .build();

        enrollmentRepository.save(enrollment);
        log.info(
                "User {} purchased course {} with {} points",
                user.getId(),
                course.getId(),
                coursePoints
        );

        int totalLessons = course.getChapters()
                .stream()
                .mapToInt(chapter -> chapter.getLessons().size())
                .sum();

        CourseProgress progress = CourseProgress.builder()
                .user(user)
                .course(course)
                .completedLessons(0)
                .totalLessons(totalLessons)
                .progressPercent(0)
                .completed(false)
                .build();

        courseProgressRepository.save(progress);

        notificationService.createNotification(
                teacher,
                user,
                "New course enrollment",
                user.getFullName() + " enrolled in \"" + course.getTitle() + "\"",
                "/teacher/revenue/courses/" + course.getId() + "/students");

        return enrollmentMapper.toBuyCourseResponse(enrollment);
    }
}
