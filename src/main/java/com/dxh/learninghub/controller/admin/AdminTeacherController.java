package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.dto.response.admin.TeacherApplicationDetailResponse;
import com.dxh.learninghub.service.interfac.admin.AdminTeacherService;
import com.dxh.learninghub.service.interfac.admin.AdminUserService;
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
@RequestMapping("/admin/teachers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Teachers", description = "APIs for administrators to manage teachers and applications")
public class AdminTeacherController {

    AdminUserService adminUserService;
    AdminTeacherService adminTeacherService;

    @Operation(summary = "Get all teachers", description = "Return a paginated list of teacher accounts")
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getAllTeachers(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy) {

        Pageable pageable = createTeacherPageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully get teacher list")
                .result(adminUserService.searchUsers(
                        pageable, null, null, "TEACHER", null, null))
                .build();
    }

    @Operation(summary = "Search teachers", description = "Search teacher accounts by name")
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> searchTeachers(
            @RequestParam String fullName,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy) {

        Pageable pageable = createTeacherPageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully search teachers")
                .result(adminUserService.searchUsers(
                        pageable, null, fullName, "TEACHER", null, null))
                .build();
    }

    @Operation(summary = "Get application details", description = "Return teacher application details for a user")
    @GetMapping("/{userId}/details")
    public ApiResponse<TeacherApplicationDetailResponse> getUserApplicationDetail(@PathVariable Long userId) {
        return ApiResponse.<TeacherApplicationDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved user application detail")
                .result(adminTeacherService.getUserApplicationDetail(userId))
                .build();
    }

    @Operation(summary = "Get pending applications", description = "Return pending teacher applications for review")
    @GetMapping("/applications")
    public ApiResponse<PageResponse<UserResponse>> getPendingTeacherApplications(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy) {

        Pageable pageable = createTeacherPageable(pageNo, pageSize, sortBy);

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully get pending applications")
                .result(adminUserService.getPendingTeacherApplications(pageable))
                .build();
    }

    @Operation(summary = "Approve teacher application", description = "Approve a pending application and grant the teacher role")
    @PostMapping("/{userId}/approve")
    public ApiResponse<?> approveTeacherRegistration(@PathVariable Long userId) {
        adminTeacherService.approveTeacherRegistration(userId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Teacher registration approved successfully")
                .build();
    }

    @Operation(summary = "Reject teacher application", description = "Reject a pending teacher application")
    @PostMapping("/{userId}/reject")
    public ApiResponse<?> rejectTeacherRegistration(@PathVariable Long userId) {
        adminTeacherService.rejectTeacherRegistration(userId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Teacher registration rejected successfully")
                .build();
    }

    @Operation(summary = "Remove teacher role", description = "Remove the teacher role from a user account")
    @PutMapping("/{userId}/remove-role")
    public ApiResponse<?> removeTeacherRole(@PathVariable Long userId) {
        adminTeacherService.removeTeacherRole(userId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Teacher role removed successfully")
                .build();
    }

    private Pageable createTeacherPageable(
            Integer pageNo,
            Integer pageSize,
            String sortBy) {
        return PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "username", "email", "fullName", "yearsOfExperience",
                "registrationStatus", "createdAt", "updatedAt");
    }
}
