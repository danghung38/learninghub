package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.CourseLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CourseUpdateRequest(
        @Size(min = 1, max = 255, message = "CONTENT_TOO_LONG")
        String title,

        @Size(min = 1, message = "INVALID_BLANK")
        String description,

        CourseLevel courseLevel,

        @Size(min = 1, max = 255, message = "CONTENT_TOO_LONG")
        String language,

        @Size(min = 1, max = 255, message = "CONTENT_TOO_LONG")
        String videoUrl,

        @Min(value = 1, message = "MIN_INVALID")
        Integer duration,

        @Min(value = 1, message = "MIN_INVALID")
        Long points
) {
}
