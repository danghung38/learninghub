package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ForgotPasswordRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Email(message = "INVALID_EMAIL")
    String email
) {}
