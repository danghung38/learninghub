package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuyCourseResponse(
    Long courseId,

    String title,

    String author,

    CourseLevel courseLevel,

    String thumbnail,

    Long points,

    LocalDateTime createdAt
) {}
