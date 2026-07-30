package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.service.interfac.admin.AdminCourseService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
            @RequestParam(required = false) String[] course,
            @RequestParam(required = false) String[] author) {

        Pageable pageable = createCoursePageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Search course successfully")
                .result(adminCourseService.searchCourses(pageable, course, author))
                .build();
    }

    @Operation(summary = "Get courses by status", description = "Return courses filtered by the selected status")
    @GetMapping("/status/{status}")
    public ApiResponse<PageResponse<CourseResponse>> getByStatus(
            @PathVariable CourseStatus status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy) {

        Pageable pageable = createCoursePageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get course successfully")
                .result(adminCourseService.getByStatus(status, pageable))
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

    @Operation(summary = "Ban a course", description = "Ban a course and make it unavailable")
    @PatchMapping("/{id}/ban")
    public ApiResponse<Void> ban(@PathVariable Long id) {

        adminCourseService.ban(id);

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
            description = "Reject a pending course, return its updated state, and notify the teacher")
    @PatchMapping("/{id}/reject")
    public ApiResponse<CourseResponse> reject(@PathVariable Long id) {
        return ApiResponse.<CourseResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Reject successfully")
                .result(adminCourseService.reject(id))
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
