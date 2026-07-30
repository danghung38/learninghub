package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.NotificationResponse;
import com.dxh.learninghub.dto.response.NotificationUnreadCountResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notifications", description = "APIs for the current user's notifications")
public class NotificationController {
    NotificationService notificationService;

    @Operation(summary = "Get my notifications", description = "Return the current user's paginated notifications")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Pageable pageable = PageUtil.createPageable(pageNo, pageSize, "createdAt:desc");
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get notifications successfully")
                .result(notificationService.getMyNotifications(pageable))
                .build();
    }

    @Operation(summary = "Get unread count", description = "Return the number of unread notifications for the current user")
    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> getMyUnreadCount() {
        return ApiResponse.<NotificationUnreadCountResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get unread notification count successfully")
                .result(notificationService.getMyUnreadCount())
                .build();
    }

    @Operation(summary = "Mark notification as read", description = "Mark one of the current user's notifications as read")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long notificationId) {
        return ApiResponse.<NotificationResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Notification marked as read")
                .result(notificationService.markAsRead(notificationId))
                .build();
    }

    @Operation(summary = "Mark all as read", description = "Mark all notifications belonging to the current user as read")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("All notifications marked as read")
                .build();
    }

    @Operation(summary = "Delete a notification", description = "Delete a notification belonging to the current user")
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteMyNotification(
            @PathVariable Long notificationId) {
        notificationService.deleteMyNotification(notificationId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Notification deleted successfully")
                .build();
    }
}
