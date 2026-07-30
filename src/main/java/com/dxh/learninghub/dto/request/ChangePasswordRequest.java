package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
public record ChangePasswordRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String currentPassword,

    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String newPassword,

    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String confirmPassword
) {}
