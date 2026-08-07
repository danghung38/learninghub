package com.dxh.learninghub.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminRevenueOverviewResponse(
        Long totalCourseSalesPoints,
        Long totalEnrollments,
        Long completedDepositCount,
        BigDecimal completedDepositAmount,
        Long completedDepositPoints,
        Long paidWithdrawalCount,
        BigDecimal paidWithdrawalAmount,
        Long paidWithdrawalPoints,
        Long pendingWithdrawalCount,
        BigDecimal pendingWithdrawalAmount,
        Long pendingWithdrawalPoints,
        BigDecimal netCashFlow
) {
}
