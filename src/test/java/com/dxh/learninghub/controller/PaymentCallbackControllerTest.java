package com.dxh.learninghub.controller;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.payment.PayOSWebhookResponse;
import com.dxh.learninghub.dto.payment.VNPayIpnResponse;
import com.dxh.learninghub.dto.payment.VNPayReturnResponse;
import com.dxh.learninghub.service.interfac.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private VNPayProperties vnPayProperties;

    @Test
    void vnpayIpn_passesAllCallbackParametersToService() throws Exception {
        when(paymentService.processIpn(org.mockito.ArgumentMatchers.<String, String>anyMap()))
                .thenReturn(new VNPayIpnResponse("00", "Confirm Success"));

        mockMvc.perform(get("/payment-callbacks/vnpay/ipn")
                        .param("vnp_TxnRef", "DEP-001")
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    void vnpayReturn_redirectsToConfiguredFrontendWithResult() throws Exception {
        when(paymentService.processReturn(org.mockito.ArgumentMatchers.<String, String>anyMap()))
                .thenReturn(new VNPayReturnResponse("DEP-001", true));
        when(vnPayProperties.getFrontendReturnUrl())
                .thenReturn("https://learninghub.example/payment-result");

        mockMvc.perform(get("/payment-callbacks/vnpay/return")
                        .param("vnp_TxnRef", "DEP-001"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://learninghub.example/payment-result?transactionRef=DEP-001&signatureValid=true"));
    }

    @Test
    void payosWebhook_withValidPayload_returnsProviderAcknowledgement() throws Exception {
        when(paymentService.processPayOSWebhook(any(Webhook.class)))
                .thenReturn(new PayOSWebhookResponse("00", "Webhook processed successfully"));

        WebhookData webhookData = WebhookData.builder()
                .orderCode(1001L)
                .amount(100_000L)
                .description("Point deposit")
                .accountNumber("123456789")
                .reference("BANK-001")
                .transactionDateTime("2026-08-17 12:00:00")
                .currency("VND")
                .paymentLinkId("LINK-001")
                .code("00")
                .desc("success")
                .build();
        Webhook webhook = new Webhook("00", "success", true, webhookData, "signature");

        mockMvc.perform(post("/payment-callbacks/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.desc").value("Webhook processed successfully"));

        verify(paymentService).processPayOSWebhook(any(Webhook.class));
    }

    @Test
    void payosWebhook_withMalformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/payment-callbacks/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }
}
