package com.dxh.learninghub.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserUpdateResponse(
    String avatar,

    String fullName,

    String gender,

    LocalDate dob,

    String address,

    String description,

    String bio,

    String expertise,

    Double yearsOfExperience,

    String facebookLink
) {}
