package com.dxh.learninghub.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCheckoutResponse(
        String transactionRef,
        BigDecimal amount,
        Long points,
        String paymentUrl,
        LocalDateTime expiresAt
) {
}
