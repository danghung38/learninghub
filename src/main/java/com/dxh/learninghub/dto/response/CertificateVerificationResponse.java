package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificateVerificationResponse(
        String verificationCode,
        String recipient,
        String courseName,
        String instructor,
        LocalDate issueDate,
        boolean valid) {
}
