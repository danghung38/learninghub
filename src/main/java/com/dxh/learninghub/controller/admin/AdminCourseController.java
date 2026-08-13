package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.request.RejectRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.service.interfac.admin.AdminCourseService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/course")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Courses", description = "APIs for administrators to review and manage courses")
public class AdminCourseController {

    AdminCourseService adminCourseService;

    @Operation(summary = "Search all courses", description = "Search and filter courses across all statuses")
    @GetMapping
    public ApiResponse<PageResponse<CourseResponse>> searchCourses(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @ParameterObject @Valid CourseSearchFilterRequest filter) {

        Pageable pageable = createCoursePageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Search course successfully")
                .result(adminCourseService.searchCourses(pageable, filter))
                .build();
    }

    @Operation(
            summary = "Approve a course",
            description = "Approve a pending course, return its updated state, and notify the teacher")
    @PatchMapping("/{id}/approve")
    public ApiResponse<CourseResponse> approve(@PathVariable Long id) {
        return ApiResponse.<CourseResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Approve successfully")
                .result(adminCourseService.approve(id))
                .build();
    }

    @Operation(summary = "Ban a course", description = "Ban a course, make it unavailable, and notify the teacher with a reason")
    @PatchMapping("/{id}/ban")
    public ApiResponse<Void> ban(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {

        adminCourseService.ban(id, request.reason());

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Ban successfully")
                .build();
    }

    @Operation(summary = "Unban a course", description = "Unban a course and return it to draft so the teacher can review and resubmit it")
    @PatchMapping("/{id}/unban")
    public ApiResponse<Void> unban(@PathVariable Long id) {

        adminCourseService.unban(id);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Unban successfully; course returned to draft")
                .build();
    }

    @Operation(
            summary = "Reject a course",
            description = "Reject a pending course, return its updated state, and notify the teacher with a reason")
    @PatchMapping("/{id}/reject")
    public ApiResponse<CourseResponse> reject(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {

        return ApiResponse.<CourseResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Reject successfully")
                .result(adminCourseService.reject(id, request.reason()))
                .build();
    }

    private Pageable createCoursePageable(
            Integer pageNo,
            Integer pageSize,
            String sortBy) {
        return PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "title", "points", "duration",
                "totalEnrollments", "status", "createdAt", "updatedAt");
    }
}
