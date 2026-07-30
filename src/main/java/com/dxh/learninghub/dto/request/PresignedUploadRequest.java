package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PresignedUploadRequest(
    @NotBlank(message = "INVALID_BLANK")
    String fileName,

    @NotNull(message = "INVALID_NULL")
    @Positive(message = "INVALID_FILE_SIZE")
    Long fileSize
) {}
