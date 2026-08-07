package com.dxh.learninghub.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminRevenueDetailResponse(
        String period,
        String month,
        Integer year,
        Long courseSalesPoints,
        Long enrollments,
        BigDecimal depositAmount,
        Long depositPoints,
        BigDecimal withdrawalAmount,
        Long withdrawalPoints
) {
}
