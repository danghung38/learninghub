package com.dxh.learninghub.controller;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.request.CreateVNPayDepositRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.VNPayIpnResponse;
import com.dxh.learninghub.dto.response.VNPayPaymentResponse;
import com.dxh.learninghub.dto.response.VNPayReturnResponse;
import com.dxh.learninghub.service.interfac.VNPayPaymentService;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "VNPAY Payments", description = "APIs for VNPAY point deposits and payment callbacks")
public class VNPayPaymentController {

    private final VNPayPaymentService vnPayPaymentService;
    private final VNPayProperties properties;

    @Operation(summary = "Create a VNPAY deposit", description = "Create a VNPAY payment URL for depositing points")
    @PostMapping("/deposits")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createDeposit(
            @Valid @RequestBody CreateVNPayDepositRequest request,
            HttpServletRequest servletRequest) {
        ApiResponse<VNPayPaymentResponse> response = ApiResponse.<VNPayPaymentResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create VNPAY payment successfully")
                .result(vnPayPaymentService.createDeposit(request, servletRequest))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Process VNPAY IPN", description = "Validate and process the server-to-server VNPAY notification")
    @GetMapping("/ipn")
    public VNPayIpnResponse ipn(@RequestParam Map<String, String> params) {
        return vnPayPaymentService.processIpn(params);
    }

    @Operation(summary = "Process VNPAY return", description = "Validate the browser return parameters and redirect to the frontend result page")
    @GetMapping("/return")
    public ResponseEntity<Void> paymentReturn(
            @RequestParam Map<String, String> params) {
        VNPayReturnResponse result = vnPayPaymentService.processReturn(params);
        URI redirectUri = UriComponentsBuilder
                .fromUriString(properties.getFrontendReturnUrl())
                .queryParam("transactionRef", result.transactionRef())
                .queryParam("signatureValid", result.signatureValid())
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @Operation(summary = "Get deposit status", description = "Return one VNPAY deposit belonging to the current user")
    @GetMapping("/deposits/{transactionRef}")
    public ApiResponse<PaymentSummaryResponse> getPayment(
            @PathVariable String transactionRef) {
        return ApiResponse.<PaymentSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get VNPAY payment successfully")
                .result(vnPayPaymentService.getPayment(transactionRef))
                .build();
    }

    @Operation(summary = "Get my deposits", description = "Filter and paginate the current user's VNPAY deposit history")
    @GetMapping("/deposits")
    public ApiResponse<PageResponse<PaymentSummaryResponse>> getMyPayments(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy,
            @RequestParam(required = false) PaymentStatus status) {
        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "amount", "status", "paymentMethod", "expiresAt",
                "paidAt", "createdAt", "updatedAt");
        return ApiResponse.<PageResponse<PaymentSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get VNPAY payments successfully")
                .result(vnPayPaymentService.getMyPayments(status, pageable))
                .build();
    }
}
