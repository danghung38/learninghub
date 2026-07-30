package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.*;
import com.dxh.learninghub.dto.response.*;
import com.dxh.learninghub.enums.RateLimitEnum;
import com.dxh.learninghub.service.interfac.UserService;
import com.dxh.learninghub.validator.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Users", description = "APIs for user registration, verification, recovery, and profiles")
public class UserController {
    UserService userService;

    @Operation(summary = "Register a user", description = "Create a user account and send an email verification code")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(action = RateLimitEnum.REGISTER, maxRequests = 20, durationMinutes = 5)
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Successfully created new user")
                .result(userService.createUser(request))
                .build();
    }

    @Operation(summary = "Resend verification code", description = "Send a new registration verification code to the user's email")
    @PostMapping("/resend-verification")
    @RateLimit(action = RateLimitEnum.RESEND_VERIFY, maxRequests = 3, durationMinutes = 5)
    public ApiResponse<?> resendVerification(@RequestBody @Valid ResendRegisterRequest request) {
        userService.resendVerification(request.email());
        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("Verification email resent successfully")
                .build();
    }

    @Operation(summary = "Update my profile", description = "Update the current user's profile and optionally replace the avatar")
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<UserUpdateResponse> updateUser(
            @RequestPart("user") @Valid UserUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.<UserUpdateResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully update user")
                .result(userService.updateMyUser(request, file))
                .build();
    }

    @Operation(summary = "Request password reset", description = "Send a password reset code to the registered email")
    @PostMapping("/forgot-password")
    @RateLimit(action = RateLimitEnum.FORGOT_PASSWORD, maxRequests = 5, durationMinutes = 5)
    ApiResponse<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Send otp reset password successful")
                .build();
    }

    @Operation(summary = "Reset password", description = "Reset an account password using a valid verification code")
    @PostMapping("/reset-password")
    @RateLimit(action = RateLimitEnum.RESET_PASSWORD, maxRequests = 20, durationMinutes = 5)
    ApiResponse<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Reset password successful")
                .build();
    }

    //xác thực tài khoản
    @Operation(summary = "Verify registration", description = "Verify a newly registered account using its email code")
    @RateLimit(action = RateLimitEnum.VERIFY_EMAIL, maxRequests = 20, durationMinutes = 5)
    @PostMapping("/verify-register")
    public ApiResponse<?> confirmEmail(@RequestBody @Valid VerifyOtpRequest request) {

        userService.verifyRegister(request.otp());

        return ApiResponse.builder()
                .code(HttpStatus.OK.value())
                .message("Verify email successful")
                .build();
    }

    @Operation(summary = "Get my profile", description = "Return the current authenticated user's profile")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully retrieved current user info")
                .result(userService.getMyInfo())
                .build();
    }

}
