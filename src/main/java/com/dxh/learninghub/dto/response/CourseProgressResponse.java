package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseProgressResponse(
    Integer completedLessons,

    Integer totalLessons,

    Integer progressPercent,

    Boolean completed,

    List<Long> completedLessonIds
) {}
