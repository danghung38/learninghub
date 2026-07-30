package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FavoriteResponse(
    Long id,

    Long courseId,

    String courseTitle,

    String thumbnail,

    String authorName,

    Long points,

    CourseLevel courseLevel,

    CourseStatus status
) implements Serializable {}
