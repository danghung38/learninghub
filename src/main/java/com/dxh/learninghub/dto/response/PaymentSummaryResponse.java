package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentSummaryResponse(
    Long id,

    String transactionRef,

    String gatewayTransactionNo,

    PaymentMethod paymentMethod,

    PaymentStatus status,

    BigDecimal amount,

    Long pointsReceived,

    String responseCode,

    String bankCode,

    LocalDateTime expiresAt,

    LocalDateTime paidAt,

    LocalDateTime createdAt
) {}
