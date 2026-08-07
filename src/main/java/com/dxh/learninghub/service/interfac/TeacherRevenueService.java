package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.RevenueReportResponse;
import com.dxh.learninghub.dto.response.TeacherDashboardResponse;

public interface TeacherRevenueService {
    TeacherDashboardResponse getRevenueOverview();
    RevenueReportResponse getRevenueReport(RevenueAnalyticsRequest request);
}
