package com.dxh.learninghub.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ChapterManagementPreviewResponse(
        Long id,
        String chapterName,
        String description,
        List<LessonResponse> lessons
) {
}
