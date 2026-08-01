package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.admin.AdminRevenueOverviewResponse;
import com.dxh.learninghub.dto.response.admin.AdminRevenueReportResponse;

public interface AdminRevenueService {

    AdminRevenueOverviewResponse getOverview();

    AdminRevenueReportResponse getReport(RevenueAnalyticsRequest request);
}
