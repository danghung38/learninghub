package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.constant.AppConstant;
import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.admin.AdminRevenueDetailResponse;
import com.dxh.learninghub.dto.response.admin.AdminRevenueOverviewResponse;
import com.dxh.learninghub.dto.response.admin.AdminRevenueReportResponse;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.enums.WithdrawalStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.PaymentRepository;
import com.dxh.learninghub.repo.WithdrawalRepository;
import com.dxh.learninghub.service.interfac.admin.AdminRevenueService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminRevenueServiceImpl implements AdminRevenueService {

    EnrollmentRepository enrollmentRepository;
    PaymentRepository paymentRepository;
    WithdrawalRepository withdrawalRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminRevenueOverviewResponse getOverview() {
        BigDecimal completedDepositAmount = safeAmount(
                paymentRepository.sumAmountByStatusForAdmin(PaymentStatus.COMPLETED));
        BigDecimal paidWithdrawalAmount = safeAmount(
                withdrawalRepository.sumAmountByStatusForAdmin(WithdrawalStatus.PAID));

        return AdminRevenueOverviewResponse.builder()
                .totalCourseSalesPoints(safeLong(enrollmentRepository.sumSpentPointsForAdmin()))
                .totalEnrollments(safeLong(enrollmentRepository.countAllEnrollments()))
                .completedDepositCount(safeLong(paymentRepository.countByStatusForAdmin(PaymentStatus.COMPLETED)))
                .completedDepositAmount(completedDepositAmount)
                .completedDepositPoints(safeLong(
                        paymentRepository.sumPointsByStatusForAdmin(PaymentStatus.COMPLETED)))
                .paidWithdrawalCount(safeLong(withdrawalRepository.countByStatusForAdmin(WithdrawalStatus.PAID)))
                .paidWithdrawalAmount(paidWithdrawalAmount)
                .paidWithdrawalPoints(safeLong(
                        withdrawalRepository.sumPointsByStatusForAdmin(WithdrawalStatus.PAID)))
                .pendingWithdrawalCount(safeLong(
                        withdrawalRepository.countByStatusForAdmin(WithdrawalStatus.PENDING)))
                .pendingWithdrawalAmount(safeAmount(
                        withdrawalRepository.sumAmountByStatusForAdmin(WithdrawalStatus.PENDING)))
                .pendingWithdrawalPoints(safeLong(
                        withdrawalRepository.sumPointsByStatusForAdmin(WithdrawalStatus.PENDING)))
                .netCashFlow(completedDepositAmount.subtract(paidWithdrawalAmount))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminRevenueReportResponse getReport(RevenueAnalyticsRequest request) {
        validateRequest(request);
        List<AdminRevenueDetailResponse> details = request.month() == null
                ? buildMonthlyDetails(request.year())
                : buildWeeklyDetails(request.year(), request.month());

        long totalCourseSalesPoints = details.stream()
                .mapToLong(item -> safeLong(item.courseSalesPoints()))
                .sum();
        long totalEnrollments = details.stream()
                .mapToLong(item -> safeLong(item.enrollments()))
                .sum();
        long totalDepositPoints = details.stream()
                .mapToLong(item -> safeLong(item.depositPoints()))
                .sum();
        BigDecimal totalDepositAmount = details.stream()
                .map(AdminRevenueDetailResponse::depositAmount)
                .map(AdminRevenueServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalWithdrawalPoints = details.stream()
                .mapToLong(item -> safeLong(item.withdrawalPoints()))
                .sum();
        BigDecimal totalWithdrawalAmount = details.stream()
                .map(AdminRevenueDetailResponse::withdrawalAmount)
                .map(AdminRevenueServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminRevenueReportResponse.builder()
                .year(request.year())
                .month(request.month())
                .totalCourseSalesPoints(totalCourseSalesPoints)
                .totalEnrollments(totalEnrollments)
                .totalDepositPoints(totalDepositPoints)
                .totalDepositAmount(totalDepositAmount)
                .totalWithdrawalPoints(totalWithdrawalPoints)
                .totalWithdrawalAmount(totalWithdrawalAmount)
                .netCashFlow(totalDepositAmount.subtract(totalWithdrawalAmount))
                .details(details)
                .build();
    }

    private List<AdminRevenueDetailResponse> buildMonthlyDetails(Integer year) {
        Map<Integer, RevenueBucket> buckets = new HashMap<>();
        mergeEnrollments(buckets, enrollmentRepository.sumSpentPointsGroupByMonthForAdmin(year));
        mergeDeposits(buckets, paymentRepository.sumCompletedPaymentsGroupByMonthForAdmin(year));
        mergeWithdrawals(buckets, withdrawalRepository.sumPaidWithdrawalsGroupByMonthForAdmin(year));

        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> toDetail(
                        buckets.getOrDefault(month, new RevenueBucket()),
                        AppConstant.MONTH_NAMES[month - 1],
                        null,
                        year))
                .toList();
    }

    private List<AdminRevenueDetailResponse> buildWeeklyDetails(Integer year, Integer month) {
        Map<Integer, RevenueBucket> buckets = new HashMap<>();
        mergeEnrollmentsByWeek(
                buckets,
                enrollmentRepository.sumSpentPointsGroupByDayOfMonthForAdmin(year, month));
        mergeDepositsByWeek(
                buckets,
                paymentRepository.sumCompletedPaymentsGroupByDayOfMonthForAdmin(year, month));
        mergeWithdrawalsByWeek(
                buckets,
                withdrawalRepository.sumPaidWithdrawalsGroupByDayOfMonthForAdmin(year, month));

        int totalWeeks = (YearMonth.of(year, month).lengthOfMonth() - 1) / 7 + 1;
        return java.util.stream.IntStream.range(0, totalWeeks)
                .mapToObj(week -> toDetail(
                        buckets.getOrDefault(week, new RevenueBucket()),
                        "Week " + (week + 1),
                        AppConstant.MONTH_NAMES[month - 1],
                        year))
                .toList();
    }

    private void mergeEnrollments(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, row[0]);
            bucket.courseSalesPoints += number(row[1]);
            bucket.enrollments += number(row[2]);
        }
    }

    private void mergeDeposits(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, row[0]);
            bucket.depositAmount = bucket.depositAmount.add(amount(row[1]));
            bucket.depositPoints += number(row[2]);
        }
    }

    private void mergeWithdrawals(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, row[0]);
            bucket.withdrawalAmount = bucket.withdrawalAmount.add(amount(row[1]));
            bucket.withdrawalPoints += number(row[2]);
        }
    }

    private void mergeEnrollmentsByWeek(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, weekIndex(row[0]));
            bucket.courseSalesPoints += number(row[1]);
            bucket.enrollments += number(row[2]);
        }
    }

    private void mergeDepositsByWeek(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, weekIndex(row[0]));
            bucket.depositAmount = bucket.depositAmount.add(amount(row[1]));
            bucket.depositPoints += number(row[2]);
        }
    }

    private void mergeWithdrawalsByWeek(Map<Integer, RevenueBucket> buckets, List<Object[]> rows) {
        for (Object[] row : rows) {
            RevenueBucket bucket = bucketFor(buckets, weekIndex(row[0]));
            bucket.withdrawalAmount = bucket.withdrawalAmount.add(amount(row[1]));
            bucket.withdrawalPoints += number(row[2]);
        }
    }

    private static RevenueBucket bucketFor(Map<Integer, RevenueBucket> buckets, Object key) {
        return buckets.computeIfAbsent((int) number(key), ignored -> new RevenueBucket());
    }

    private static int weekIndex(Object day) {
        return (int) ((number(day) - 1) / 7);
    }

    private static AdminRevenueDetailResponse toDetail(
            RevenueBucket bucket,
            String period,
            String month,
            Integer year) {
        return AdminRevenueDetailResponse.builder()
                .period(period)
                .month(month)
                .year(year)
                .courseSalesPoints(bucket.courseSalesPoints)
                .enrollments(bucket.enrollments)
                .depositAmount(bucket.depositAmount)
                .depositPoints(bucket.depositPoints)
                .withdrawalAmount(bucket.withdrawalAmount)
                .withdrawalPoints(bucket.withdrawalPoints)
                .build();
    }

    private void validateRequest(RevenueAnalyticsRequest request) {
        if (request == null || request.year() == null) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }

        int currentYear = Year.now().getValue();
        if (request.year() < 2020 || request.year() > currentYear) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }
        if (request.month() != null && (request.month() < 1 || request.month() > 12)) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal amount(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private static BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class RevenueBucket {
        long courseSalesPoints;
        long enrollments;
        BigDecimal depositAmount = BigDecimal.ZERO;
        long depositPoints;
        BigDecimal withdrawalAmount = BigDecimal.ZERO;
        long withdrawalPoints;
    }
}
