package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record AdvertisementUpdateRequest(
        @Size(min = 1, max = 100, message = "CONTENT_TOO_LONG") String title,
        String description,
        @Size(min = 1, max = 255, message = "CONTENT_TOO_LONG")
        @URL(message = "INVALID_URL") String link,
        Long courseId,
        LocalDate startDate,
        LocalDate endDate) {
}
