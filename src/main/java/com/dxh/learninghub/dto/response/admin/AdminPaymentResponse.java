package com.dxh.learninghub.dto.response.admin;

import com.dxh.learninghub.dto.response.PaymentSummaryResponse;

public record AdminPaymentResponse(
        Long userId,
        String username,
        String userFullName,
        PaymentSummaryResponse payment
) {
}
