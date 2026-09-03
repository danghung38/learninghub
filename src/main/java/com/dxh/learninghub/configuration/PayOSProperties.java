package com.dxh.learninghub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.payos")
public class PayOSProperties {

    private String returnUrl = "http://localhost:5173/payment-result";
    private String cancelUrl = "http://localhost:5173/payment-result";
}
