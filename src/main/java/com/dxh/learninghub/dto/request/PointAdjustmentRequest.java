package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PointAdjustmentRequest(
    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Long points,

    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 255, message = "CONTENT_TOO_LONG")
    String description
) {}
