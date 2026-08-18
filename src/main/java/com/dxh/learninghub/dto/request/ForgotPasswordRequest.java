package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ForgotPasswordRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Email(message = "INVALID_EMAIL")
    String email,

    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 2048, message = "CONTENT_TOO_LONG")
    String turnstileToken
) {}
