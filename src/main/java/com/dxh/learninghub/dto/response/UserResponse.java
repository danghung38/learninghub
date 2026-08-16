package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.Gender;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
    Long id,

    String email,

    String fullName,

    String username,

    String avatar,

    String phoneNumber,

    Gender gender,

    Boolean enabled,

    Boolean banned,

    RegistrationStatus registrationStatus,

    Long points,

    String address,

    String cvUrl,

    String expertise,

    String bio,

    String certificateUrl,

    String facebookLink,

    String description,

    Double yearsOfExperience,

    LocalDate dob,

    Set<RoleResponse> roles,

    LocalDateTime lastLogin
) implements Serializable {}
