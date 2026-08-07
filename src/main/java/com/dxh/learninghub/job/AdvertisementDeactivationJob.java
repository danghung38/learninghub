package com.dxh.learninghub.job;

import com.dxh.learninghub.repo.AdvertisementRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(
        prefix = "jobs.advertisement-deactivation",
        name = "enabled",
        havingValue = "true")
public class AdvertisementDeactivationJob {

    final AdvertisementRepository advertisementRepository;

    @Value("${jobs.advertisement-deactivation.zone:Asia/Ho_Chi_Minh}")
    String zone;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        log.info("Triggering advertisement deactivation on application startup...");
        deactivateExpiredAdvertisements();
    }

    @Transactional
    @Scheduled(
            cron = "${jobs.advertisement-deactivation.cron:0 0 0 * * *}",
            zone = "${jobs.advertisement-deactivation.zone:Asia/Ho_Chi_Minh}")
    public void deactivateExpiredAdvertisements() {
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        int updatedCount = advertisementRepository.deactivateExpiredAdvertisements(today);

        log.info(
                "Advertisement deactivation job completed: deactivatedCount={}, currentDate={}",
                updatedCount,
                today);
    }
}