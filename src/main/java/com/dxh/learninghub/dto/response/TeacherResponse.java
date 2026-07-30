package com.dxh.learninghub.dto.response;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;


@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherResponse(
    Long id,

    String fullName,

    String email,

    String expertise,

    Double yearsOfExperience,

    String bio,

    String facebookLink,

    String cvUrl,

    String certificateUrl,

    RegistrationStatus registrationStatus
) {}
