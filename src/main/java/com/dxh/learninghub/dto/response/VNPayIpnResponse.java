package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VNPayIpnResponse(
        @JsonProperty("RspCode") String responseCode,
        @JsonProperty("Message") String message
) {
}
