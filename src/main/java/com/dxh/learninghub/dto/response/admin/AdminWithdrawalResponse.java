package com.dxh.learninghub.dto.response.admin;

import com.dxh.learninghub.enums.WithdrawalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminWithdrawalResponse(
        Long id,
        Long teacherId,
        String teacherName,
        Long bankAccountId,
        String bankName,
        String accountNumber,
        String accountHolder,
        BigDecimal amount,
        Long points,
        WithdrawalStatus status,
        LocalDateTime createdAt,
        String paymentProofUrl,
        String rejectionReason
) {
}
