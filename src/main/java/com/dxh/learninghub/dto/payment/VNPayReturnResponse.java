package com.dxh.learninghub.dto.payment;

public record VNPayReturnResponse(
        String transactionRef,
        boolean signatureValid
) {
}
