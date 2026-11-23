package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.MobileForgotPasswordRequest;
import com.dxh.learninghub.dto.request.MobileUserCreationRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.enums.RateLimitEnum;
import com.dxh.learninghub.service.interfac.UserService;
import com.dxh.learninghub.validator.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Mobile Users", description = "Public account APIs for the native mobile application")
public class MobileUserController {

    UserService userService;

    @Operation(summary = "Register from mobile", description = "Create a mobile user account without a Turnstile token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(action = RateLimitEnum.REGISTER, maxRequests = 20, durationMinutes = 5)
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid MobileUserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Successfully created new user")
                .result(userService.createMobileUser(request))
                .build();
    }

    @Operation(summary = "Request a mobile password reset", description = "Send a reset code without a Turnstile token")
    @PostMapping("/forgot-password")
    @RateLimit(action = RateLimitEnum.FORGOT_PASSWORD, maxRequests = 5, durationMinutes = 5)
    public ApiResponse<Void> forgotPassword(@RequestBody @Valid MobileForgotPasswordRequest request) {
        userService.forgotPasswordMobile(request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Send otp reset password successful")
                .build();
    }
}
