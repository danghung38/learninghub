package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.AuthenticationRequest;
import com.dxh.learninghub.dto.request.ChangePasswordRequest;
import com.dxh.learninghub.dto.request.GoogleLoginRequest;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void login_withValidCredentials_returnsTokensAndPassesClientIp() throws Exception {
        AuthenticationResponse response = AuthenticationResponse.builder()
                .authenticated(true)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .username("student")
                .role("USER")
                .build();
        when(authenticationService.login(any(AuthenticationRequest.class), eq("203.0.113.10")))
                .thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthenticationRequest.builder()
                                        .username("student")
                                        .password("secret123")
                                        .turnstileToken("turnstile-token")
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.authenticated").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"));

        verify(authenticationService).login(any(AuthenticationRequest.class), eq("203.0.113.10"));
    }

    @Test
    void login_withBlankCredentials_returnsValidationErrorAndSkipsService() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthenticationRequest.builder().username("").password("").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_BLANK.getCode()));
    }

    @Test
    void login_whenCredentialsAreInvalid_returnsMappedBusinessError() throws Exception {
        when(authenticationService.login(any(AuthenticationRequest.class), any(String.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthenticationRequest.builder()
                                        .username("student")
                                        .password("wrong-password")
                                        .turnstileToken("turnstile-token")
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
    }

    @Test
    void googleLogin_withValidToken_returnsApplicationTokens() throws Exception {
        when(authenticationService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .thenReturn(AuthenticationResponse.builder()
                        .authenticated(true)
                        .accessToken("google-access")
                        .build());

        mockMvc.perform(post("/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GoogleLoginRequest.builder().idToken("google-id-token").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("google-access"));
    }

    @Test
    void changePassword_withShortPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("123", "456", "456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PASSWORD.getCode()));
    }
}
