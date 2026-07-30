package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LogoutRequest(
    @NotBlank(message = "INVALID_BLANK")
    String accessToken,

    @NotBlank(message = "INVALID_BLANK")
    String refreshToken
) {}
