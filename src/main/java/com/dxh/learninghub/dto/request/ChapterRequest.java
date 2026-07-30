package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ChapterRequest(
    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Long courseId,

    @NotBlank(message = "INVALID_BLANK")
    String chapterName,

    @NotBlank(message = "INVALID_BLANK")
    String description
) {}
