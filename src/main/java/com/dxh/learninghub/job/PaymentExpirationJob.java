package com.dxh.learninghub.job;

import com.dxh.learninghub.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "jobs.payment-expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentExpirationJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;

    @Scheduled(
            initialDelayString = "${jobs.payment-expiration.initial-delay-ms:60000}",
            fixedDelayString = "${jobs.payment-expiration.fixed-delay-ms:60000}")
    @Transactional
    public void expirePendingPayments() {
        int updated = paymentRepository.expirePendingPayments(
                LocalDateTime.now(ZONE));
        if (updated > 0) {
            log.info("Marked {} pending payment(s) as expired", updated);
        }
    }
}
