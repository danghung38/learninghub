package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.ForgotPasswordRequest;
import com.dxh.learninghub.dto.request.UserCreationRequest;
import com.dxh.learninghub.dto.request.VerifyOtpRequest;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_withValidRequest_returnsCreatedUser() throws Exception {
        when(userService.createUser(any(UserCreationRequest.class)))
                .thenReturn(UserResponse.builder()
                        .id(21L)
                        .username("nguyenvana")
                        .email("nguyenvana@example.com")
                        .build());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Successfully created new user"))
                .andExpect(jsonPath("$.result.id").value(21));
    }

    @Test
    void createUser_withInvalidEmail_returnsValidationError() throws Exception {
        UserCreationRequest invalidRequest = UserCreationRequest.builder()
                .username("nguyenvana")
                .password("secret123")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("not-an-email")
                .address("Hà Nội")
                .gender("MALE")
                .dob(LocalDate.of(2000, 1, 1))
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_EMAIL.getCode()));
    }

    @Test
    void createUser_whenUsernameAlreadyExists_returnsMappedError() throws Exception {
        when(userService.createUser(any(UserCreationRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_EXISTED));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_EXISTED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_EXISTED.getMessage()));
    }

    @Test
    void forgotPassword_withRegisteredEmail_callsService() throws Exception {
        doNothing().when(userService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ForgotPasswordRequest.builder()
                                        .email("nguyenvana@example.com")
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Send otp reset password successful"));

        verify(userService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    void verifyRegister_withBlankOtp_returnsValidationError() throws Exception {
        mockMvc.perform(post("/users/verify-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VerifyOtpRequest.builder().otp("").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_BLANK.getCode()));
    }

    @Test
    void getMyInfo_returnsCurrentUser() throws Exception {
        when(userService.getMyInfo()).thenReturn(UserResponse.builder()
                .id(21L)
                .username("nguyenvana")
                .build());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(21));
    }

    private static UserCreationRequest validRegistration() {
        return UserCreationRequest.builder()
                .username("nguyenvana")
                .password("secret123")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@example.com")
                .address("Hà Nội")
                .gender("MALE")
                .dob(LocalDate.of(2000, 1, 1))
                .build();
    }
}
