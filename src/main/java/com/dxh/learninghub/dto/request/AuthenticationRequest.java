package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AuthenticationRequest(
    @NotBlank(message = "INVALID_BLANK")
    String username,

    @NotBlank(message = "INVALID_BLANK")
    String password,

    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 2048, message = "CONTENT_TOO_LONG")
    String turnstileToken
) {}
