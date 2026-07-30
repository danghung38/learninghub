package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseUploadResponse(
    Long id,

    String author,

    String title,

    String description,

    CourseLevel courseLevel,

    Integer duration,

    String language,

    Long points,

    CourseStatus status,

    String thumbnail,

    String videoUrl
) {}
