package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.MobileForgotPasswordRequest;
import com.dxh.learninghub.dto.request.MobileUserCreationRequest;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.service.interfac.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MobileUserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserService userService;

    @Test
    void register_withoutTurnstileToken_returnsCreatedUser() throws Exception {
        when(userService.createMobileUser(any(MobileUserCreationRequest.class)))
                .thenReturn(UserResponse.builder().id(31L).username("mobile01").build());

        mockMvc.perform(post("/mobile/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value(31));
    }

    @Test
    void forgotPassword_withoutTurnstileToken_callsMobileService() throws Exception {
        mockMvc.perform(post("/mobile/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MobileForgotPasswordRequest.builder()
                                .email("student@example.com")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Send otp reset password successful"));

        verify(userService).forgotPasswordMobile(any(MobileForgotPasswordRequest.class));
    }

    private static MobileUserCreationRequest validRegistration() {
        return MobileUserCreationRequest.builder()
                .username("mobile01")
                .password("secret123")
                .fullName("Mobile Student")
                .phoneNumber("0912345678")
                .email("student@example.com")
                .address("Ha Noi")
                .gender("MALE")
                .dob(LocalDate.of(2000, 1, 1))
                .build();
    }
}
