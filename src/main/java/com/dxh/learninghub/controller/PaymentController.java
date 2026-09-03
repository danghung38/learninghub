package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.payment.PaymentCheckoutResponse;
import com.dxh.learninghub.dto.request.CreateDepositRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.service.interfac.PaymentService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User-facing payment operations: create a deposit and view deposit history. */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payments", description = "User payment and point deposit APIs")
public class PaymentController {

    PaymentService paymentService;

    @Operation(summary = "Create a deposit", description = "Create a hosted checkout URL using the selected payment method")
    @PostMapping("/deposits")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> createDeposit(
            @Valid @RequestBody CreateDepositRequest request,
            HttpServletRequest servletRequest) {
        ApiResponse<PaymentCheckoutResponse> response = ApiResponse.<PaymentCheckoutResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create payment successfully")
                .result(paymentService.createDeposit(request, servletRequest))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get deposit status", description = "Return one deposit belonging to the current user")
    @GetMapping("/deposits/{transactionRef}")
    public ApiResponse<PaymentSummaryResponse> getPayment(
            @PathVariable String transactionRef) {
        return ApiResponse.<PaymentSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get payment successfully")
                .result(paymentService.getPayment(transactionRef))
                .build();
    }

    @Operation(summary = "Get my deposits", description = "Filter and paginate the current user's deposit history")
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
                .message("Get payments successfully")
                .result(paymentService.getMyPayments(status, pageable))
                .build();
    }
}
