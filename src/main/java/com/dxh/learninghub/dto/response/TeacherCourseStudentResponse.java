package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.EnrollmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TeacherCourseStudentResponse(
    Long userId,

    String fullName,

    String avatar,

    String courseTitle,

    EnrollmentStatus enrollmentStatus,

    LocalDateTime enrolledAt
) {}
