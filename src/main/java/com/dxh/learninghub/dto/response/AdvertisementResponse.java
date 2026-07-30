package com.dxh.learninghub.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvertisementResponse(
        Long id,
        String title,
        String description,
        String image,
        String link,
        Long courseId,
        String courseTitle,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt) {
}
