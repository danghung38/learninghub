package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.RevenueAnalyticsRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.RevenueReportResponse;
import com.dxh.learninghub.dto.response.TeacherDashboardResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.TeacherCourseStudentResponse;
import com.dxh.learninghub.service.interfac.TeacherRevenueService;
import com.dxh.learninghub.service.interfac.TeacherService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/revenue")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teacher Revenue", description = "APIs for teacher revenue reports and students")
public class TeacherRevenueController {

    TeacherRevenueService teacherRevenueService;
    TeacherService teacherService;

    @Operation(
            summary = "Get revenue overview",
            description = "Return dashboard totals for the current teacher's courses, students, revenue, and reviews"
    )
    @GetMapping
    public ApiResponse<TeacherDashboardResponse> getRevenueOverview() {
        return ApiResponse.<TeacherDashboardResponse>builder()
                .code(HttpStatus.OK.value())
                .result(teacherRevenueService.getRevenueOverview())
                .build();
    }

    @Operation(
            summary = "Get revenue report",
            description = "Return total revenue with monthly or weekly revenue details"
    )
    @PostMapping("/report")
    public ApiResponse<RevenueReportResponse> getRevenueReport(
            @Valid @RequestBody RevenueAnalyticsRequest request) {
        return ApiResponse.<RevenueReportResponse>builder()
                .code(HttpStatus.OK.value())
                .result(teacherRevenueService.getRevenueReport(request))
                .build();
    }

    @Operation(summary = "Get course students", description = "Return students enrolled in a course owned by the teacher")
    @GetMapping("/courses/{courseId}/students")
    public ApiResponse<PageResponse<TeacherCourseStudentResponse>> getCourseStudents(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageUtil.createPageable(pageNo, pageSize, null);
        return ApiResponse.<PageResponse<TeacherCourseStudentResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get course students successfully")
                .result(teacherService.getCourseStudents(courseId, pageable))
                .build();
    }
}
