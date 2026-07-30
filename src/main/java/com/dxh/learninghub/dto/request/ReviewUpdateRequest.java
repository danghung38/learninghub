package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewUpdateRequest(
        @Size(min = 1, max = 2000, message = "CONTENT_TOO_LONG")
        String content,

        @Min(value = 1, message = "MIN_INVALID")
        @Max(value = 5, message = "MAX_INVALID")
        Integer rating
) {
}
