package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.LessonContentType;
import lombok.Builder;

@Builder
public record LessonAIRequest(
        Long id,

        String lessonName,

        LessonContentType contentType,

        String transcript
) {
}
