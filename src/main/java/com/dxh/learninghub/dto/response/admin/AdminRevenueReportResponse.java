package com.dxh.learninghub.dto.response.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AdminRevenueReportResponse(
        Integer year,
        Integer month,
        Long totalCourseSalesPoints,
        Long totalEnrollments,
        Long totalDepositPoints,
        BigDecimal totalDepositAmount,
        Long totalWithdrawalPoints,
        BigDecimal totalWithdrawalAmount,
        BigDecimal netCashFlow,
        List<AdminRevenueDetailResponse> details
) {
}
