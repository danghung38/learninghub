package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BuyCourseRequest(
    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Long courseId
) {}
