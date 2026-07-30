package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseResponse(
    Long id,

    Long authorId,

    String author,

    String title,

    String description,

    Integer duration,

    String language,

    CourseStatus status,

    CourseLevel courseLevel,

    String thumbnail,

    String videoUrl,

    Long points,

    Long totalEnrollments
) {}
