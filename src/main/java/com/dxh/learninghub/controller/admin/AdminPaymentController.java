package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.admin.AdminPaymentResponse;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.service.interfac.PaymentService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Payments", description = "APIs for administrators to inspect payment records")
public class AdminPaymentController {

    PaymentService paymentService;

    @Operation(summary = "Get all payments", description = "Filter and paginate payment records across all users")
    @GetMapping
    public ApiResponse<PageResponse<AdminPaymentResponse>> getPayments(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "amount", "status", "paymentMethod", "expiresAt",
                "paidAt", "createdAt", "updatedAt");
        return ApiResponse.<PageResponse<AdminPaymentResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get payments successfully")
                .result(paymentService.getPaymentsForAdmin(
                        userId, status, method, from, to, pageable))
                .build();
    }

    @Operation(summary = "Get payment details", description = "Return a payment by its transaction reference")
    @GetMapping("/{transactionRef}")
    public ApiResponse<AdminPaymentResponse> getPayment(
            @PathVariable String transactionRef) {
        return ApiResponse.<AdminPaymentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get payment successfully")
                .result(paymentService.getPaymentForAdmin(transactionRef))
                .build();
    }
}
