package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.PointTransactionType;
import lombok.Builder;

@Builder
public record PointAdjustmentResponse(
    Long userId,

    Long transactionId,

    Long changedPoints,

    Long currentPoints,

    PointTransactionType transactionType
) {}
