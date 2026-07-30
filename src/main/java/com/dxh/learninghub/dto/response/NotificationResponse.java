package com.dxh.learninghub.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
    Long id,

    Long userId,

    String title,

    String message,

    Boolean isRead,

    String url,

    String avatarUrl,

    LocalDateTime createdAt
) {}
