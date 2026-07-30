package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record IntrospectRequest(
    @NotBlank(message = "INVALID_BLANK")
    String token
) {}
