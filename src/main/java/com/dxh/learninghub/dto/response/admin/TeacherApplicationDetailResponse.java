package com.dxh.learninghub.dto.response.admin;

import com.dxh.learninghub.dto.response.RoleResponse;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Set;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherApplicationDetailResponse(
    Long id,

    String fullName,

    String email,

    String phoneNumber,

    String gender,

    String avatar,

    LocalDate dob,

    String cvUrl,

    String certificate,

    String facebookLink,

    String description,

    String expertise,

    String bio,

    Double yearsOfExperience,

    RegistrationStatus registrationStatus,

    Long points,

    Set<RoleResponse> roles
) {}
