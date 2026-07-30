package com.dxh.learninghub.job;

import com.dxh.learninghub.repo.NotificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(
        prefix = "jobs.notification-cleanup",
        name = "enabled",
        havingValue = "true")
public class NotificationCleanupJob {

    final NotificationRepository notificationRepository;

    @Value("${jobs.notification-cleanup.retention-days:60}")
    int retentionDays;

    @Value("${jobs.notification-cleanup.zone:Asia/Ho_Chi_Minh}")
    String zone;

    @Transactional
    @Scheduled(
            cron = "${jobs.notification-cleanup.cron:0 0 3 * * *}",
            zone = "${jobs.notification-cleanup.zone:Asia/Ho_Chi_Minh}")
    public void deleteExpiredNotifications() {
        if (retentionDays < 1) {
            log.warn("Notification cleanup skipped: retention-days must be at least 1");
            return;
        }

        LocalDateTime expiredAt = LocalDateTime.now(ZoneId.of(zone))
                .minusDays(retentionDays);
        int deletedCount = notificationRepository.deleteAllExpiredBefore(expiredAt);

        log.info(
                "Notification cleanup completed: deleted={}, expiredBefore={}",
                deletedCount,
                expiredAt);
    }
}
