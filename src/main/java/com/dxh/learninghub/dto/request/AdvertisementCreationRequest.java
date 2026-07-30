package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record AdvertisementCreationRequest(
        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 100, message = "CONTENT_TOO_LONG") String title,
        String description,
        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 255, message = "CONTENT_TOO_LONG")
        @URL(message = "INVALID_URL") String link,
        Long courseId,
        @NotNull(message = "INVALID_NULL") LocalDate startDate,
        @NotNull(message = "INVALID_NULL") LocalDate endDate) {
}
