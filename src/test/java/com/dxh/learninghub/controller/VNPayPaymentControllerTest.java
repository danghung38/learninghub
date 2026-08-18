package com.dxh.learninghub.controller;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.request.CreateVNPayDepositRequest;
import com.dxh.learninghub.dto.response.VNPayPaymentResponse;
import com.dxh.learninghub.dto.response.VNPayReturnResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.VNPayPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VNPayPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VNPayPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VNPayPaymentService vnPayPaymentService;

    @MockBean
    private VNPayProperties properties;

    @Test
    void createDeposit_withValidAmount_returnsCreatedPayment() throws Exception {
        when(vnPayPaymentService.createDeposit(any(CreateVNPayDepositRequest.class),
                any(HttpServletRequest.class)))
                .thenReturn(new VNPayPaymentResponse(
                        "DEP-001",
                        BigDecimal.valueOf(100_000),
                        100L,
                        "https://sandbox.vnpay.vn/pay",
                        LocalDateTime.of(2026, 8, 17, 12, 0)));

        mockMvc.perform(post("/payments/vnpay/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVNPayDepositRequest(100_000L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.transactionRef").value("DEP-001"))
                .andExpect(jsonPath("$.result.points").value(100));
    }

    @Test
    void createDeposit_withAmountBelowMinimum_returnsValidationError() throws Exception {
        mockMvc.perform(post("/payments/vnpay/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVNPayDepositRequest(999L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MIN_INVALID.getCode()));
    }

    @Test
    void getPayment_whenTransactionDoesNotExist_returnsMappedError() throws Exception {
        when(vnPayPaymentService.getPayment("UNKNOWN"))
                .thenThrow(new AppException(ErrorCode.PAYMENT_NOT_EXISTED));

        mockMvc.perform(get("/payments/vnpay/deposits/{transactionRef}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAYMENT_NOT_EXISTED.getCode()));
    }

    @Test
    void paymentReturn_redirectsToConfiguredFrontendWithResult() throws Exception {
        when(vnPayPaymentService.processReturn(org.mockito.ArgumentMatchers.<String, String>anyMap()))
                .thenReturn(new VNPayReturnResponse("DEP-001", true));
        when(properties.getFrontendReturnUrl())
                .thenReturn("https://learninghub.example/payment-result");

        mockMvc.perform(get("/payments/vnpay/return")
                        .param("vnp_TxnRef", "DEP-001"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://learninghub.example/payment-result?transactionRef=DEP-001&signatureValid=true"));
    }

    @Test
    void ipn_passesAllCallbackParametersToService() throws Exception {
        when(vnPayPaymentService.processIpn(org.mockito.ArgumentMatchers.<String, String>anyMap()))
                .thenReturn(new com.dxh.learninghub.dto.response.VNPayIpnResponse("00", "Confirm Success"));

        mockMvc.perform(get("/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "DEP-001")
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }
}
