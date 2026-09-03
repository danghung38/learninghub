package com.dxh.learninghub.configuration;

import vn.payos.PayOS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class PayOSConfig {

    @Bean
    @Lazy
    public PayOS payOS() {
        return PayOS.fromEnv();
    }
}
