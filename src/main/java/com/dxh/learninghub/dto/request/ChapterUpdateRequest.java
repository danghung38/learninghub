package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ChapterUpdateRequest(
    @Size(min = 1, message = "INVALID_BLANK")
    String chapterName,

    @Size(min = 1, message = "INVALID_BLANK")
    String description
) {}
