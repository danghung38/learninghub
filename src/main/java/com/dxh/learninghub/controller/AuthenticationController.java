package com.dxh.learninghub.controller;


import com.dxh.learninghub.dto.request.*;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.dto.response.IntrospectResponse;
import com.dxh.learninghub.service.interfac.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Authentication", description = "APIs for login, tokens, logout, and account passwords")
public class AuthenticationController {
    AuthenticationService authenticationService;

    //tạo token khi login
    @Operation(summary = "Log in", description = "Authenticate credentials and issue access and refresh tokens")
    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request, HttpServletRequest servletRequest){
        String ip = servletRequest.getRemoteAddr();
        var result = authenticationService.login(request, ip);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @Operation(summary = "Introspect a token", description = "Validate a token and return its active status")
    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody @Valid IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .code(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @Operation(
            summary = "Change password",
            description = "Change the authenticated user's password after verifying the current password"
    )
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        authenticationService.changePassword(request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Password changed successfully")
                .build();
    }

    @Operation(
            summary = "Create password",
            description = "Create a password for an authenticated account that does not have one"
    )
    @PostMapping("/password")
    public ApiResponse<Void> createPassword(
            @RequestBody @Valid CreatePasswordRequest request
    ) {
        authenticationService.createPassword(request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Password created successfully")
                .build();
    }

    @Operation(summary = "Log out", description = "Blacklist the supplied access and refresh tokens")
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .build();
    }

    @Operation(summary = "Refresh tokens", description = "Issue new tokens from a valid refresh token")
    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid RefreshRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @Operation(summary = "Log in with Google", description = "Authenticate with a Google ID token and issue application tokens")
    @PostMapping("/login/google")
    ApiResponse<AuthenticationResponse> loginWithGoogle(@RequestBody @Valid GoogleLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .code(HttpStatus.OK.value())
                .result(authenticationService.loginWithGoogle(request))
                .build();
    }
}
