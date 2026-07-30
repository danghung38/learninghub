package com.dxh.learninghub.dto.response;

public record VNPayReturnResponse(
        String transactionRef,
        boolean signatureValid
) {
}
