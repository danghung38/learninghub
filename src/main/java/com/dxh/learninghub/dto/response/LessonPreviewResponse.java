package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.LessonContentType;
import lombok.Builder;

@Builder
public record LessonPreviewResponse(
    Long id,

    String lessonName,

    LessonContentType contentType,

    String description
) {}
