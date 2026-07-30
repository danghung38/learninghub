package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
    @NotBlank(message = "INVALID_BLANK")
    String resetCode,

    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String newPassword
) {}
