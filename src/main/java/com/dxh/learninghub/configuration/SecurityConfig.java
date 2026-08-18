package com.dxh.learninghub.configuration;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {


    static final String[] PUBLIC_POST_ENDPOINTS = {
            "/auth/login", "/auth/introspect", "/auth/logout", "/auth/refresh", "/users","/auth/login/google"
            ,"/users/reset-password","/users/verify-register","/users/forgot-password","/users/resend-verification"
    };

    static final String[] PUBLIC_GET_ENDPOINTS = {
            "/advertisements",
            "/courses/list",
            "/courses/teacher/**",
            "/teachers/public/**",
            "/courses/title",
            "/courses/{courseId}/preview",
            "/courses/{courseId}",
            "/courses/{courseId}/reviews",
            "/courses/{courseId}/rating-summary",
            "/certificates/verify/**",
            "/payments/vnpay/ipn",
            "/payments/vnpay/return",
            "/actuator/health",
            "/actuator/info",
            "/actuator/health/**"
    };

    static final String[] API_DOC_ENDPOINTS = {
            "/api/swagger-ui.html",
            "/api/swagger-ui/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v3/api-docs/**",
            "/api/swagger-resources/**",
            "/api/webjars/**",
            "/users/confirm-email"

    };



    CustomJwtDecoder customJwtDecoder;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                                .requestMatchers(API_DOC_ENDPOINTS).permitAll()
                                .requestMatchers("/ws/**", "/topic/**", "/queue/**", "/user/**").permitAll()
                                .requestMatchers("/admin-notify.html", "/admin-notify-fail.html").permitAll()
                                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwtConfigurer ->
                                        jwtConfigurer.decoder(customJwtDecoder)
                                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                );

        return httpSecurity.build();
    }



    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

}
