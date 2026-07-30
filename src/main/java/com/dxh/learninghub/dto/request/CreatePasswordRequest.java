package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreatePasswordRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String password,

    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String confirmPassword
) {}
