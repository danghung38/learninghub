package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;
import java.util.Set;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChapterResponse(
    Long id,

    Long courseId,

    String chapterName,

    String description
) implements Serializable {}
