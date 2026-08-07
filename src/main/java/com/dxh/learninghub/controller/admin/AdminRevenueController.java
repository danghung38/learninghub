package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.admin.AdminRevenueOverviewResponse;
import com.dxh.learninghub.dto.response.admin.AdminRevenueReportResponse;
import com.dxh.learninghub.service.interfac.admin.AdminRevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Revenue", description = "APIs for platform revenue and cash-flow reporting")
public class AdminRevenueController {

    AdminRevenueService adminRevenueService;

    @Operation(
            summary = "Get revenue overview",
            description = "Return platform sales, deposits, teacher payouts, pending withdrawals, and net cash flow"
    )
    @GetMapping
    public ApiResponse<AdminRevenueOverviewResponse> getOverview() {
        return ApiResponse.<AdminRevenueOverviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get admin revenue overview successfully")
                .result(adminRevenueService.getOverview())
                .build();
    }

    @Operation(
            summary = "Get revenue report",
            description = "Return monthly or weekly platform revenue details for a selected year"
    )
    @PostMapping("/report")
    public ApiResponse<AdminRevenueReportResponse> getReport(
            @Valid @RequestBody RevenueAnalyticsRequest request) {
        return ApiResponse.<AdminRevenueReportResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get admin revenue report successfully")
                .result(adminRevenueService.getReport(request))
                .build();
    }
}
