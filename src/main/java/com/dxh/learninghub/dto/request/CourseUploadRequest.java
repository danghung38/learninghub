package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.CourseLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CourseUploadRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 255, message = "CONTENT_TOO_LONG")
    String title,

    @NotBlank(message = "INVALID_BLANK")
    String description,

    @NotNull(message = "INVALID_NULL")
    CourseLevel courseLevel,

    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 255, message = "CONTENT_TOO_LONG")
    String language,

    @Size(max = 255, message = "CONTENT_TOO_LONG")
    String videoUrl,

    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Integer duration,

    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Long points
) {}
