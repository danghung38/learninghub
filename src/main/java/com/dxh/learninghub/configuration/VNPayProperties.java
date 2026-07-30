package com.dxh.learninghub.configuration;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "payment.vnpay")
public class VNPayProperties {
    @NotBlank
    private String payUrl;

    @NotBlank
    private String returnUrl;

    @NotBlank
    private String frontendReturnUrl;

    @NotBlank
    private String tmnCode;

    @NotBlank
    private String hashSecret;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";

    @Positive
    private long amountPerPoint = 1_000L;

    @Positive
    private long expireMinutes = 15L;
}
