package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(
    boolean authenticated,

    String accessToken,

    String refreshToken,

    String role,

    String username,

    String fullName
) implements Serializable {}
