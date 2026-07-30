package com.dxh.learninghub.dto.request;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.validator.EnumValue;
import lombok.Builder;

@Builder
public record UserSearchFilterRequest(
        String username,
        String fullName,

        @EnumValue(enumClass = RoleEnum.class, message = "INVALID_ROLE_ENUM")
        String role,
        Boolean banned,
        Boolean enabled
) {}