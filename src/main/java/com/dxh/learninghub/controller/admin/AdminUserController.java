package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.UserSearchFilterRequest;
import com.dxh.learninghub.dto.response.*;
import com.dxh.learninghub.service.interfac.admin.AdminUserService;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Users", description = "APIs for administrators to manage users and roles")
public class AdminUserController {
    AdminUserService adminUserService;

    @Operation(
            summary = "Get user details",
            description = "Return detailed account information for a selected user"
    )
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long userId) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully get user details")
                .result(adminUserService.getUserById(userId))
                .build();
    }

    @Operation(summary = "Ban a user", description = "Ban a user account by ID")
    @PostMapping("/ban/{userId}")
    public ApiResponse<?> banUser(@PathVariable Long userId) {
        adminUserService.banUser(userId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User banned successfully")
                .build();
    }

    @Operation(summary = "Unban a user", description = "Restore access to a banned user account")
    @PostMapping("/unban/{userId}")
    public ApiResponse<?> unbanUser(@PathVariable Long userId) {
        adminUserService.unbanUser(userId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User unbanned successfully")
                .build();
    }

    @Operation(
            summary = "Add role to user",
            description = "Add a role without removing the user's existing roles"
    )
    @PostMapping("/{userId}/roles/{roleName}")
    public ApiResponse<Void> addRole(
            @PathVariable Long userId,
            @PathVariable String roleName
    ) {
        adminUserService.addRole(userId, roleName);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Role added to user successfully")
                .build();
    }

    @Operation(
            summary = "Remove role from user",
            description = "Remove a specific role without affecting other roles"
    )
    @DeleteMapping("/{userId}/roles/{roleName}")
    public ApiResponse<Void> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName
    ) {
        adminUserService.removeRole(userId, roleName);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Role removed from user successfully")
                .build();
    }


    @Operation(
            summary = "Search users",
            description = "Filter users by username, full name, role, banned and enabled status"
    )
    @GetMapping("/list")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @ParameterObject @ModelAttribute @Valid UserSearchFilterRequest filter) {

        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "username", "email", "fullName", "points", "enabled",
                "banned", "registrationStatus", "createdAt", "updatedAt");

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully get user list")
                .result(adminUserService.searchUsers(pageable, filter))
                .build();
    }


}
