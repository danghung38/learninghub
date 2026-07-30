package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BankAccountResponse(
        Long id,
        String bankName,
        String accountNumber,
        String accountHolder,
        Boolean isDefault,
        Boolean active,
        LocalDateTime createdAt
) {
}
