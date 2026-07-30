package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RevenueAnalyticsRequest(
    @NotNull(message = "INVALID_NULL")
    @Min(value = 2020, message = "MIN_INVALID")
    Integer year,

    @Min(value = 1, message = "MIN_INVALID")
    @Max(value = 12, message = "MAX_INVALID")
    Integer month
) {}
