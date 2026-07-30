package com.dxh.learninghub.controller;


import com.dxh.learninghub.dto.request.BuyCourseRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.BuyCourseResponse;
import com.dxh.learninghub.dto.response.EnrollmentStatusResponse;
import com.dxh.learninghub.dto.response.MyCourseResponse;
import com.dxh.learninghub.service.interfac.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Enrollments", description = "APIs for purchasing courses and tracking enrollment status")
public class EnrollmentController {
    EnrollmentService enrollmentService;

    @Operation(summary = "Buy a course", description = "Purchase a course using the current user's points")
    @PostMapping("/buy")
    public ApiResponse<BuyCourseResponse> buyCourse(
            @RequestBody @Valid BuyCourseRequest request) {

        return ApiResponse.<BuyCourseResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Buy course successfully")
                .result(enrollmentService.buyCourse(request))
                .build();
    }

    @Operation(summary = "Get my enrolled courses", description = "Return courses purchased by the current user")
    @GetMapping("/my-courses")
    ApiResponse<List<MyCourseResponse>> getCourseByCurrentUser() {
        return ApiResponse.<List<MyCourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("My Courses")
                .result(enrollmentService.getCourseByUserCurrent())
                .build();
    }

    @Operation(
            summary = "Get enrollment status",
            description = "Return the current user's enrollment state and progress for a course"
    )
    @GetMapping("/courses/{courseId}/status")
    public ApiResponse<EnrollmentStatusResponse> getEnrollmentStatus(
            @PathVariable Long courseId) {
        return ApiResponse.<EnrollmentStatusResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get enrollment status successfully")
                .result(enrollmentService.getEnrollmentStatus(courseId))
                .build();
    }
}
