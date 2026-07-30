package com.dxh.learninghub.dto.response;

import lombok.Builder;

@Builder
public record NotificationUnreadCountResponse(
    long unreadCount
) {}
