package com.dxh.learninghub.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EmailConfig {
    //email service
    @Bean
    public WebClient brevoWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
