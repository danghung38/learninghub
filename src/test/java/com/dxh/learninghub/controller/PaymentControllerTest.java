package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.payment.PaymentCheckoutResponse;
import com.dxh.learninghub.dto.request.CreateDepositRequest;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.PaymentService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createDeposit_forSelectedPayOSMethod_returnsCreatedPayment() throws Exception {
        when(paymentService.createDeposit(any(CreateDepositRequest.class), any(HttpServletRequest.class)))
                .thenReturn(new PaymentCheckoutResponse(
                        "PAYOS-001",
                        BigDecimal.valueOf(100_000),
                        100L,
                        "https://pay.payos.vn/web/PAYOS-001",
                        LocalDateTime.of(2026, 8, 17, 12, 0)));

        mockMvc.perform(post("/payments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateDepositRequest(100_000L, PaymentMethod.PAYOS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Create payment successfully"))
                .andExpect(jsonPath("$.result.transactionRef").value("PAYOS-001"))
                .andExpect(jsonPath("$.result.points").value(100));
    }

    @Test
    void createDeposit_withAmountBelowMinimum_returnsValidationError() throws Exception {
        mockMvc.perform(post("/payments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateDepositRequest(999L, PaymentMethod.VNPAY))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MIN_INVALID.getCode()));
    }

    @Test
    void createDeposit_withoutPaymentMethod_returnsValidationError() throws Exception {
        mockMvc.perform(post("/payments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_NULL.getCode()));
    }

    @Test
    void getPayment_whenTransactionDoesNotExist_returnsMappedError() throws Exception {
        when(paymentService.getPayment("UNKNOWN"))
                .thenThrow(new AppException(ErrorCode.PAYMENT_NOT_EXISTED));

        mockMvc.perform(get("/payments/deposits/{transactionRef}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAYMENT_NOT_EXISTED.getCode()));
    }

    @Test
    void cancelPayOSPayment_synchronizesCanceledCheckout() throws Exception {
        mockMvc.perform(post("/payments/deposits/{transactionRef}/cancel", "1788439825436"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Cancel payment successfully"));

        org.mockito.Mockito.verify(paymentService)
                .cancelPayOSPayment("1788439825436");
    }
}
