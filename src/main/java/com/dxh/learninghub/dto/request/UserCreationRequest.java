package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.Gender;
import com.dxh.learninghub.validator.DobConstraint;
import com.dxh.learninghub.validator.GenderSubset;
import com.dxh.learninghub.validator.PhoneNumber;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserCreationRequest(
    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6,message = "USERNAME_INVALID")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "INVALID_USERNAME_FORMAT"
    )
    String username,

    @NotBlank(message = "INVALID_BLANK")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String password,

    @NotBlank(message = "INVALID_NAME")
    String fullName,

    @PhoneNumber(message = "INVALID_PHONE_NUMBER")
    @NotBlank(message = "INVALID_BLANK")
    String phoneNumber,

    @Email(message = "INVALID_EMAIL")
    @NotBlank(message = "INVALID_BLANK")
    String email,

    @NotBlank(message = "INVALID_BLANK")
    String address,

//    @EnumValue(name = "gender", enumClass = Gender.class)
    @GenderSubset(anyOf = {Gender.MALE, Gender.FEMALE, Gender.OTHER},message = "INVALID_GENDER")
    String gender,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob
) {}
