package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.CourseLevel;
import lombok.Builder;

import java.util.List;

@Builder
public record CourseAIRequest(
    Long id,

    String teacherName,

    String title,

    String description,

    Long points,

    Integer duration,

    String language,

    CourseLevel courseLevel,

    List<ChapterAIRequest> chapters
) {}
