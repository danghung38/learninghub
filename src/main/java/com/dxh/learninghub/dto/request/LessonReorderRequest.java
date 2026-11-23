package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record LessonReorderRequest(
        @NotNull(message = "INVALID_NULL")
        @Min(value = 1, message = "MIN_INVALID")
        Long chapterId,

        @NotEmpty(message = "INVALID_EMPTY")
        List<@NotNull(message = "INVALID_NULL") @Min(value = 1, message = "MIN_INVALID") Long> lessonIds
) {
}
