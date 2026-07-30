package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 2000, message = "CONTENT_TOO_LONG")
    String content,

    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    @Max(value = 5, message = "MAX_INVALID")
    Integer rating
) {}
