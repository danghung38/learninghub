package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GoogleLoginRequest(
    @NotBlank(message = "INVALID_BLANK")
    String idToken
) {}
