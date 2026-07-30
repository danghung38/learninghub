package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Set;

@Builder
public record RoleRequest(
    @NotBlank(message = "INVALID_BLANK")
    String name,

    @NotBlank(message = "INVALID_BLANK")
    String description,

    @NotEmpty(message = "INVALID_EMPTY")
    Set<String> permissions
) {}
