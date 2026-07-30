package com.dxh.learninghub.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ChapterPreviewResponse(
    Long id,

    String chapterName,

    String description,

    List<LessonPreviewResponse> lessons
) {}
