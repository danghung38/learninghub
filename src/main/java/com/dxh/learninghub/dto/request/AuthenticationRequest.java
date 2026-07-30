package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AuthenticationRequest(
    @NotBlank(message = "INVALID_BLANK")
    String username,

    @NotBlank(message = "INVALID_BLANK")
    String password
) {}
