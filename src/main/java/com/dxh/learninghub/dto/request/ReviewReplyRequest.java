package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewReplyRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(max = 2000, message = "CONTENT_TOO_LONG")
    String content
) {}
