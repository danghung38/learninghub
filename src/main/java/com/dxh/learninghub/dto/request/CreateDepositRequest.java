package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateDepositRequest(
        @NotNull(message = "INVALID_NULL")
        @Min(value = 1_000, message = "MIN_INVALID")
        @Max(value = 50_000_000L, message = "MAX_INVALID")
        Long amount,

        @NotNull(message = "INVALID_NULL")
        PaymentMethod paymentMethod
) {
}
