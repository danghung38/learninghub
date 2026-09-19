package com.dxh.learninghub.dto.request;

import lombok.Builder;

@Builder
public record LessonAIRequest(
    String lessonName,
    String contentScript
) {}
