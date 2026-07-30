package com.dxh.learninghub.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VNPayPaymentResponse(
        String transactionRef,
        BigDecimal amount,
        Long points,
        String paymentUrl,
        LocalDateTime expiresAt
) {
}
