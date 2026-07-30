package com.dxh.learninghub.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record RevenueReportResponse(
    Long totalRevenue,

    List<RevenueDetailResponse> revenueDetails
) {}
