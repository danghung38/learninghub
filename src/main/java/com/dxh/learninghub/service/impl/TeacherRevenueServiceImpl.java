package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.RevenueDetailResponse;
import com.dxh.learninghub.dto.response.RevenueReportResponse;
import com.dxh.learninghub.dto.response.TeacherDashboardResponse;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.RevenueMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.ReviewRepository;
import com.dxh.learninghub.service.interfac.TeacherRevenueService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherRevenueServiceImpl implements TeacherRevenueService {

    static final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    CurrentUserProvider currentUserProvider;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;
    RevenueMapper revenueMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public TeacherDashboardResponse getRevenueOverview() {
        Long teacherId = getCurrentTeacher().getId();

        return TeacherDashboardResponse.builder()
                .totalRevenue(enrollmentRepository.sumSpentPointsByTeacher(teacherId))
                .totalCourses(courseRepository.countCoursesByAuthorId(teacherId))
                .totalStudents(enrollmentRepository.countDistinctStudentsByTeacher(teacherId))
                .totalEnrollments(enrollmentRepository.countEnrollmentsByTeacher(teacherId))
                .totalReviews(reviewRepository.countByTeacher(teacherId))
                .averageRating(reviewRepository.averageRatingByTeacher(teacherId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public List<RevenueDetailResponse> getRevenueAnalytics(RevenueAnalyticsRequest request) {
        validateRequest(request);
        Long teacherId = getCurrentTeacher().getId();
        return buildAnalytics(teacherId, request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public RevenueReportResponse getRevenueReport(RevenueAnalyticsRequest request) {
        validateRequest(request);
        Long teacherId = getCurrentTeacher().getId();

        List<RevenueDetailResponse> details = buildAnalytics(teacherId, request);

        long totalRevenue = details.stream()
                .mapToLong(RevenueDetailResponse::revenue)
                .sum();

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueDetails(details)
                .build();
    }

    /**
     * Logic dùng chung cho analytics & report.
     * Nhận sẵn teacherId, không tự lấy user/check quyền -> tránh double auth check.
     */
    private List<RevenueDetailResponse> buildAnalytics(Long teacherId, RevenueAnalyticsRequest request) {
        return request.month() == null
                ? buildMonthlyAnalytics(teacherId, request.year())
                : buildWeeklyAnalytics(teacherId, request.year(), request.month());
    }

    private List<RevenueDetailResponse> buildMonthlyAnalytics(Long teacherId, Integer year) {
        List<Object[]> raw = enrollmentRepository.sumSpentPointsByTeacherGroupByMonth(teacherId, year);

        Map<Integer, Long> revenueByMonth = new HashMap<>();
        for (Object[] row : raw) {
            revenueByMonth.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }

        List<RevenueDetailResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(revenueMapper.toMonthlyDetail(
                    MONTH_NAMES[m - 1], year, revenueByMonth.getOrDefault(m, 0L)));
        }
        return result;
    }

    private List<RevenueDetailResponse> buildWeeklyAnalytics(Long teacherId, Integer year, Integer month) {
        List<Object[]> raw = enrollmentRepository.sumSpentPointsByTeacherGroupByDayOfMonth(teacherId, year, month);

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        int totalWeeks = (daysInMonth - 1) / 7 + 1;
        long[] revenueByWeek = new long[totalWeeks];

        for (Object[] row : raw) {
            int day = ((Number) row[0]).intValue();
            long revenue = ((Number) row[1]).longValue();
            revenueByWeek[(day - 1) / 7] += revenue;
        }

        List<RevenueDetailResponse> result = new ArrayList<>();
        for (int w = 0; w < totalWeeks; w++) {
            result.add(revenueMapper.toWeeklyDetail(
                    "Week " + (w + 1), MONTH_NAMES[month - 1], year, revenueByWeek[w]));
        }
        return result;
    }

    private void validateRequest(RevenueAnalyticsRequest request) {

        if (request == null || request.year() == null) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }

        int currentYear = Year.now().getValue();

        if (request.year() < 2020 || request.year() > currentYear) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }

        Integer month = request.month();

        if (month != null && (month < 1 || month > 12)) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }
    }

    private User getCurrentTeacher() {
        User user = currentUserProvider.getCurrentUser();
        boolean isTeacherOrAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleEnum.TEACHER.name())
                        || role.getName().equals(RoleEnum.ADMIN.name()));
        if (!isTeacherOrAdmin) {
            throw new AppException(ErrorCode.USER_NOT_TEACHER);
        }
        return user;
    }
}
