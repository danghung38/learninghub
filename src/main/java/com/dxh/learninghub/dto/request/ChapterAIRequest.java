package com.dxh.learninghub.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ChapterAIRequest(
        Long id,

        String chapterName,

        String description,

        List<LessonAIRequest> lessons
) {
}
