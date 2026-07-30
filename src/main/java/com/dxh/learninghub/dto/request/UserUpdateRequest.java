package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.Gender;
import com.dxh.learninghub.validator.DobConstraint;
import com.dxh.learninghub.validator.GenderSubset;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserUpdateRequest(
    String fullName,

    @GenderSubset(anyOf = {Gender.MALE, Gender.FEMALE, Gender.OTHER},message = "INVALID_GENDER")
    String gender,

    String address,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob,

    String description,

    @Size(max = 255, message = "CONTENT_TOO_LONG")
    @URL(message = "INVALID_FB_URL")
    String facebookLink
) {}
