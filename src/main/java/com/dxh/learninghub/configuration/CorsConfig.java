package com.dxh.learninghub.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                        .allowedOriginPatterns(
                                "https://learninghub.id.vn",
                                "http://localhost:5173"
                        )  // Chỉ định rõ các domain được phép
                        .allowCredentials(true)      // Nếu FE gửi cookie
                        .allowedHeaders("*");        // Cho phép tất cả headers
            }
        };
    }
}