package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.LessonContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LessonUpdateRequest(
        @Min(value = 1, message = "MIN_INVALID")
        Long chapterId,

        @Size(min = 1, message = "INVALID_BLANK")
        String lessonName,

        LessonContentType contentType,

        @Size(min = 1, max = 255, message = "CONTENT_TOO_LONG")
        String contentUrl,

        @Size(max = 255, message = "CONTENT_TOO_LONG")
        String description
) {
}
