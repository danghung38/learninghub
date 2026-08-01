package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AdvertisementUpdateRequest(
        @Size(min = 1, max = 100, message = "CONTENT_TOO_LONG") String title,
        String description,
        @NotNull(message = "INVALID_NULL")
        @Positive(message = "INVALID_POSITIVE") Long courseId,
        LocalDate startDate,
        LocalDate endDate) {
}
