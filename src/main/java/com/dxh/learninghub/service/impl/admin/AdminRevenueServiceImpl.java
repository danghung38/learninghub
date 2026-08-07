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
import java.util.ArrayList;
import java.util.List;

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
        var completedDepositAmount = safeAmount(paymentRepository.sumAmountByStatusForAdmin(PaymentStatus.COMPLETED));
        var paidWithdrawalAmount = safeAmount(withdrawalRepository.sumAmountByStatusForAdmin(WithdrawalStatus.PAID));

        return AdminRevenueOverviewResponse.builder()
                .totalCourseSalesPoints(safeLong(enrollmentRepository.sumSpentPointsForAdmin()))
                .totalEnrollments(safeLong(enrollmentRepository.countAllEnrollments()))
                .completedDepositCount(safeLong(paymentRepository.countByStatusForAdmin(PaymentStatus.COMPLETED)))
                .completedDepositAmount(completedDepositAmount)
                .completedDepositPoints(safeLong(paymentRepository.sumPointsByStatusForAdmin(PaymentStatus.COMPLETED)))
                .paidWithdrawalCount(safeLong(withdrawalRepository.countByStatusForAdmin(WithdrawalStatus.PAID)))
                .paidWithdrawalAmount(paidWithdrawalAmount)
                .paidWithdrawalPoints(safeLong(withdrawalRepository.sumPointsByStatusForAdmin(WithdrawalStatus.PAID)))
                .pendingWithdrawalCount(safeLong(withdrawalRepository.countByStatusForAdmin(WithdrawalStatus.PENDING)))
                .pendingWithdrawalAmount(safeAmount(withdrawalRepository.sumAmountByStatusForAdmin(WithdrawalStatus.PENDING)))
                .pendingWithdrawalPoints(safeLong(withdrawalRepository.sumPointsByStatusForAdmin(WithdrawalStatus.PENDING)))
                .netCashFlow(completedDepositAmount.subtract(paidWithdrawalAmount))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminRevenueReportResponse getReport(RevenueAnalyticsRequest request) {
        validateRequest(request);

        List<AdminRevenueDetailResponse> details = (request.month() == null)
                ? buildMonthlyDetails(request.year())
                : buildWeeklyDetails(request.year(), request.month());

        BigDecimal totalDepositAmount = details.stream()
                .map(AdminRevenueDetailResponse::depositAmount)
                .map(AdminRevenueServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawalAmount = details.stream()
                .map(AdminRevenueDetailResponse::withdrawalAmount)
                .map(AdminRevenueServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminRevenueReportResponse.builder()
                .year(request.year())
                .month(request.month())
                .totalCourseSalesPoints(details.stream().mapToLong(i -> safeLong(i.courseSalesPoints())).sum())
                .totalEnrollments(details.stream().mapToLong(i -> safeLong(i.enrollments())).sum())
                .totalDepositPoints(details.stream().mapToLong(i -> safeLong(i.depositPoints())).sum())
                .totalDepositAmount(totalDepositAmount)
                .totalWithdrawalPoints(details.stream().mapToLong(i -> safeLong(i.withdrawalPoints())).sum())
                .totalWithdrawalAmount(totalWithdrawalAmount)
                .netCashFlow(totalDepositAmount.subtract(totalWithdrawalAmount))
                .details(details)
                .build();
    }

    // --- XỬ LÝ THEO THÁNG
    private List<AdminRevenueDetailResponse> buildMonthlyDetails(Integer year) {
        RevenueBucket[] buckets = new RevenueBucket[12];
        for (int i = 0; i < 12; i++) buckets[i] = new RevenueBucket();

        // 1. Nhồi dữ liệu Enrollment vào mảng theo tháng (index = tháng - 1)
        for (Object[] row : enrollmentRepository.sumSpentPointsGroupByMonthForAdmin(year)) {
            int month = (int) number(row[0]);
            if (month >= 1 && month <= 12) {
                buckets[month - 1].courseSalesPoints += number(row[1]);
                buckets[month - 1].enrollments += number(row[2]);
            }
        }

        // 2. Nhồi dữ liệu Deposit
        for (Object[] row : paymentRepository.sumCompletedPaymentsGroupByMonthForAdmin(year)) {
            int month = (int) number(row[0]);
            if (month >= 1 && month <= 12) {
                buckets[month - 1].depositAmount = buckets[month - 1].depositAmount.add(amount(row[1]));
                buckets[month - 1].depositPoints += number(row[2]);
            }
        }

        // 3. Nhồi dữ liệu Withdrawal
        for (Object[] row : withdrawalRepository.sumPaidWithdrawalsGroupByMonthForAdmin(year)) {
            int month = (int) number(row[0]);
            if (month >= 1 && month <= 12) {
                buckets[month - 1].withdrawalAmount = buckets[month - 1].withdrawalAmount.add(amount(row[1]));
                buckets[month - 1].withdrawalPoints += number(row[2]);
            }
        }

        // Chuyển mảng thành List Response
        List<AdminRevenueDetailResponse> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            result.add(toDetail(buckets[i], AppConstant.MONTH_NAMES[i], null, year));
        }
        return result;
    }

    // --- XỬ LÝ THEO TUẦN
    private List<AdminRevenueDetailResponse> buildWeeklyDetails(Integer year, Integer month) {
        int totalWeeks = (YearMonth.of(year, month).lengthOfMonth() - 1) / 7 + 1;
        RevenueBucket[] buckets = new RevenueBucket[totalWeeks];
        for (int i = 0; i < totalWeeks; i++) buckets[i] = new RevenueBucket();

        // 1. Enrollment theo tuần
        for (Object[] row : enrollmentRepository.sumSpentPointsGroupByDayOfMonthForAdmin(year, month)) {
            int week = (int) ((number(row[0]) - 1) / 7);
            if (week >= 0 && week < totalWeeks) {
                buckets[week].courseSalesPoints += number(row[1]);
                buckets[week].enrollments += number(row[2]);
            }
        }

        // 2. Deposit theo tuần
        for (Object[] row : paymentRepository.sumCompletedPaymentsGroupByDayOfMonthForAdmin(year, month)) {
            int week = (int) ((number(row[0]) - 1) / 7);
            if (week >= 0 && week < totalWeeks) {
                buckets[week].depositAmount = buckets[week].depositAmount.add(amount(row[1]));
                buckets[week].depositPoints += number(row[2]);
            }
        }

        // 3. Withdrawal theo tuần
        for (Object[] row : withdrawalRepository.sumPaidWithdrawalsGroupByDayOfMonthForAdmin(year, month)) {
            int week = (int) ((number(row[0]) - 1) / 7);
            if (week >= 0 && week < totalWeeks) {
                buckets[week].withdrawalAmount = buckets[week].withdrawalAmount.add(amount(row[1]));
                buckets[week].withdrawalPoints += number(row[2]);
            }
        }

        // Chuyển mảng thành List Response
        List<AdminRevenueDetailResponse> result = new ArrayList<>();
        for (int i = 0; i < totalWeeks; i++) {
            result.add(toDetail(buckets[i], "Week " + (i + 1), AppConstant.MONTH_NAMES[month - 1], year));
        }
        return result;
    }

    private static AdminRevenueDetailResponse toDetail(RevenueBucket b, String period, String month, Integer year) {
        return AdminRevenueDetailResponse.builder()
                .period(period).month(month).year(year)
                .courseSalesPoints(b.courseSalesPoints).enrollments(b.enrollments)
                .depositAmount(b.depositAmount).depositPoints(b.depositPoints)
                .withdrawalAmount(b.withdrawalAmount).withdrawalPoints(b.withdrawalPoints)
                .build();
    }

    private void validateRequest(RevenueAnalyticsRequest request) {
        if (request == null || request.year() == null || request.year() < 2020 || request.year() > Year.now().getValue()
                || (request.month() != null && (request.month() < 1 || request.month() > 12))) {
            throw new AppException(ErrorCode.INVALID_REVENUE_REQUEST);
        }
    }

    private static long number(Object val) { return val == null ? 0L : ((Number) val).longValue(); }
    private static long safeLong(Long val) { return val == null ? 0L : val; }
    private static BigDecimal amount(Object val) { return val == null ? BigDecimal.ZERO : new BigDecimal(val.toString()); }
    private static BigDecimal safeAmount(BigDecimal val) { return val == null ? BigDecimal.ZERO : val; }

    private static final class RevenueBucket {
        long courseSalesPoints, enrollments, depositPoints, withdrawalPoints;
        BigDecimal depositAmount = BigDecimal.ZERO, withdrawalAmount = BigDecimal.ZERO;
    }
}