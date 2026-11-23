package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.LessonContentType;
import lombok.Builder;

@Builder
public record LessonAIRequest(
        Long id,

        String lessonName,

        Integer lessonOrder,

        LessonContentType contentType,

        String transcript
) {
}
