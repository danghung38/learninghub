package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.RevenueDetailResponse;
import com.dxh.learninghub.dto.response.RevenueReportResponse;
import com.dxh.learninghub.dto.response.TeacherDashboardResponse;

import java.util.List;

public interface TeacherRevenueService {
    TeacherDashboardResponse getRevenueOverview();
    List<RevenueDetailResponse> getRevenueAnalytics(RevenueAnalyticsRequest request);
    RevenueReportResponse getRevenueReport(RevenueAnalyticsRequest request);
}