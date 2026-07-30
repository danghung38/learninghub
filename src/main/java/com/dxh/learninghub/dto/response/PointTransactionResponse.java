package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.PointTransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PointTransactionResponse(
    Long id,

    Long userId,

    String username,

    String userFullName,

    Long changedPoints,

    PointTransactionType transactionType,

    String description,

    Long courseId,

    String courseTitle,

    Long paymentId,

    PaymentSummaryResponse payment,

    LocalDateTime createdAt
) {}
