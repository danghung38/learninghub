package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdvertisementCreationRequest(
        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 100, message = "CONTENT_TOO_LONG") String title,
        String description,
        @NotNull(message = "INVALID_NULL")
        @Positive(message = "INVALID_POSITIVE") Long courseId,
        @NotNull(message = "INVALID_NULL") LocalDate startDate,
        @NotNull(message = "INVALID_NULL") LocalDate endDate) {
}
