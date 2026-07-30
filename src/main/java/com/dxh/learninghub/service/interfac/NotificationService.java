package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.NotificationResponse;
import com.dxh.learninghub.dto.response.NotificationUnreadCountResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.User;
import org.springframework.data.domain.Pageable;


public interface NotificationService {
    PageResponse<NotificationResponse> getMyNotifications(Pageable pageable);

    NotificationUnreadCountResponse getMyUnreadCount();

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsRead();

    void deleteMyNotification(Long notificationId);

    NotificationResponse createNotification(
            User receiver,
            User sender,
            String title,
            String message,
            String url);
}
