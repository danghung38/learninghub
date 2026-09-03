package com.dxh.learninghub.controller;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.payment.PayOSWebhookResponse;
import com.dxh.learninghub.dto.payment.VNPayIpnResponse;
import com.dxh.learninghub.dto.payment.VNPayReturnResponse;
import com.dxh.learninghub.service.interfac.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import vn.payos.model.webhooks.Webhook;

import java.net.URI;
import java.util.Map;

/** Server-to-server callbacks kept separate from user-facing payment operations. */
@RestController
@RequestMapping("/payment-callbacks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Callbacks", description = "Payment gateway callbacks and webhooks")
public class PaymentCallbackController {

    PaymentService paymentService;
    VNPayProperties vnPayProperties;

    @Operation(summary = "Process VNPAY IPN", description = "Validate and process the server-to-server VNPAY notification")
    @GetMapping("/vnpay/ipn")
    public VNPayIpnResponse vnpayIpn(@RequestParam Map<String, String> params) {
        return paymentService.processIpn(params);
    }

    @Operation(summary = "Process VNPAY return", description = "Validate the browser return parameters and redirect to the frontend result page")
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        VNPayReturnResponse result = paymentService.processReturn(params);
        URI redirectUri = UriComponentsBuilder
                .fromUriString(vnPayProperties.getFrontendReturnUrl())
                .queryParam("transactionRef", result.transactionRef())
                .queryParam("signatureValid", result.signatureValid())
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @Operation(summary = "Receive payOS webhook", description = "Verify and process the server-to-server payOS payment result")
    @PostMapping("/payos/webhook")
    public ResponseEntity<PayOSWebhookResponse> payosWebhook(@RequestBody Webhook webhook) {
        return ResponseEntity.ok(paymentService.processPayOSWebhook(webhook));
    }
}
