package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PermissionRequest(
    @NotBlank(message = "INVALID_BLANK")
    String name,

    @NotBlank(message = "INVALID_BLANK")
    String description
) {}
