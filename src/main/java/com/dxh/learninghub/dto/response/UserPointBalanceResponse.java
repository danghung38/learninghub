package com.dxh.learninghub.dto.response;

import lombok.Builder;

@Builder
public record UserPointBalanceResponse(
    Long points
) {}
