package com.dxh.learninghub.dto.response;

import lombok.Builder;

@Builder
public record ChatNotificationResponse(
    Long conversationId,

    MessageResponse message,

    Integer unreadCount
) {}
