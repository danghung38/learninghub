package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.MobileAuthenticationRequest;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.service.interfac.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileAuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MobileAuthenticationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthenticationService authenticationService;

    @Test
    void login_withoutTurnstileToken_returnsTokensAndPassesClientIp() throws Exception {
        when(authenticationService.loginMobile(any(MobileAuthenticationRequest.class), eq("203.0.113.20")))
                .thenReturn(AuthenticationResponse.builder()
                        .authenticated(true)
                        .accessToken("mobile-access")
                        .refreshToken("mobile-refresh")
                        .build());

        mockMvc.perform(post("/mobile/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.20");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MobileAuthenticationRequest.builder()
                                .username("student")
                                .password("secret123")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.authenticated").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("mobile-access"));

        verify(authenticationService).loginMobile(any(MobileAuthenticationRequest.class), eq("203.0.113.20"));
    }
}
