package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionResponse(
    String name,

    String description
) implements Serializable {}
