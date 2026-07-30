package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalResponse(
        Long id,
        Long bankAccountId,
        BigDecimal amount,
        Long points,
        WithdrawalStatus status,
        String bankName,
        String accountNumber,
        String accountHolder,
        LocalDateTime createdAt,
        String paymentProofUrl,
        String rejectionReason
) {
}
