package com.dxh.learninghub.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ChapterAIRequest(
    String chapterName,
    List<LessonAIRequest> lessons
) {}
