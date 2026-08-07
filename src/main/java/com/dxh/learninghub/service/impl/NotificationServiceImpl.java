package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.response.NotificationResponse;
import com.dxh.learninghub.dto.response.NotificationUnreadCountResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.Notification;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.NotificationMapper;
import com.dxh.learninghub.repo.NotificationRepository;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    NotificationRepository notificationRepository;
    NotificationMapper notificationMapper;
    CurrentUserProvider currentUserProvider;
    SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PageResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        Page<Notification> page = notificationRepository.findByUser(user, pageable);

        return PageResponse.<NotificationResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(notificationMapper::toResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public NotificationUnreadCountResponse getMyUnreadCount() {
        User user = currentUserProvider.getCurrentUser();
        long count = notificationRepository.countByUserAndIsReadFalse(user);

        return NotificationUnreadCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public NotificationResponse markAsRead(Long notificationId) {
        User user = currentUserProvider.getCurrentUser();
        Notification notification = notificationRepository
                .findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
        }

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void markAllAsRead() {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.markAllAsRead(user);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteMyNotification(Long notificationId) {
        User user = currentUserProvider.getCurrentUser();
        Notification notification = notificationRepository
                .findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(
            User receiver,
            User sender,
            String title,
            String message,
            String url) {
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .user(receiver)
                        .sender(sender)
                        .title(title.trim())
                        .message(message.trim())
                        .url(url)
                        .isRead(false)
                        .build());

        NotificationResponse response = notificationMapper.toResponse(notification);
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/notifications",
                response);
        log.info("Sent notification {} to user {} with content {}", notification.getId(), receiver.getId(), response);
        return response;
    }
}
