package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificateResponse(
        Long id,
        Long courseId,
        String courseName,
        String instructor,
        String thumbnail,
        LocalDate issueDate,
        String verificationCode
) {}
