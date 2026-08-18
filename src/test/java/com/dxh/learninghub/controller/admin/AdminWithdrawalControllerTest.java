package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.RejectWithdrawalRequest;
import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.enums.WithdrawalStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.WithdrawalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminWithdrawalController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminWithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WithdrawalService withdrawalService;

    @Test
    void getById_returnsWithdrawalForReview() throws Exception {
        when(withdrawalService.getWithdrawalById(8L)).thenReturn(withdrawal(8L));

        mockMvc.perform(get("/admin/withdrawals/{id}", 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(8))
                .andExpect(jsonPath("$.result.teacherName").value("Nguyễn Văn A"));
    }

    @Test
    void getById_whenWithdrawalDoesNotExist_returnsMappedError() throws Exception {
        when(withdrawalService.getWithdrawalById(404L))
                .thenThrow(new AppException(ErrorCode.WITHDRAWAL_NOT_EXISTED));

        mockMvc.perform(get("/admin/withdrawals/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.WITHDRAWAL_NOT_EXISTED.getCode()));
    }

    @Test
    void reject_withValidReason_callsService() throws Exception {
        when(withdrawalService.reject(any(Long.class), any(RejectWithdrawalRequest.class)))
                .thenReturn(withdrawal(8L));

        mockMvc.perform(put("/admin/withdrawals/{id}/reject", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RejectWithdrawalRequest("Sai thông tin tài khoản"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reject withdrawal successfully"));

        verify(withdrawalService).reject(any(Long.class), any(RejectWithdrawalRequest.class));
    }

    @Test
    void reject_withBlankReason_returnsValidationError() throws Exception {
        mockMvc.perform(put("/admin/withdrawals/{id}/reject", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RejectWithdrawalRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_BLANK.getCode()));
    }

    @Test
    void markAsPaid_withProofImage_returnsUpdatedWithdrawal() throws Exception {
        MockMultipartFile proof = new MockMultipartFile(
                "file", "proof.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        when(withdrawalService.markAsPaid(any(Long.class), any()))
                .thenReturn(withdrawal(8L));

        mockMvc.perform(multipart("/admin/withdrawals/{id}/paid", 8L)
                        .file(proof)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(8));
    }

    private static AdminWithdrawalResponse withdrawal(Long id) {
        return new AdminWithdrawalResponse(
                id,
                12L,
                "Nguyễn Văn A",
                20L,
                "Vietcombank",
                "0123456789",
                "NGUYEN VAN A",
                BigDecimal.valueOf(800_000),
                1_000L,
                WithdrawalStatus.PENDING,
                LocalDateTime.of(2026, 8, 17, 10, 30),
                null,
                null);
    }
}
