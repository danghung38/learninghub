package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.MobileAuthenticationRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.service.interfac.AuthenticationService;
import com.dxh.learninghub.utils.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Mobile Authentication", description = "Authentication APIs for the native mobile application")
public class MobileAuthenticationController {

    AuthenticationService authenticationService;

    @Operation(summary = "Log in from mobile", description = "Authenticate native mobile credentials without a Turnstile token")
    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(
            @RequestBody @Valid MobileAuthenticationRequest request,
            HttpServletRequest servletRequest) {
        String ip = IpUtil.getClientIp(servletRequest);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(HttpStatus.OK.value())
                .result(authenticationService.loginMobile(request, ip))
                .build();
    }
}
