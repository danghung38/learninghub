package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.LessonContentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LessonResponse(
    Long id,

    Long chapterId,

    String lessonName,

    Integer lessonOrder,

    LessonContentType contentType,

    String contentUrl,

    String description,

    String contentScript
) {}
