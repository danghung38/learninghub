package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnrollmentStatusResponse(
    Long courseId,

    boolean enrolled,

    EnrollmentStatus status
) {}
