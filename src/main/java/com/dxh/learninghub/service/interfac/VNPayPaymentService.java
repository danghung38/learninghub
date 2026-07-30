package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.CreateVNPayDepositRequest;
import com.dxh.learninghub.dto.response.VNPayIpnResponse;
import com.dxh.learninghub.dto.response.VNPayPaymentResponse;
import com.dxh.learninghub.dto.response.VNPayReturnResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.admin.AdminPaymentResponse;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface VNPayPaymentService {
    VNPayPaymentResponse createDeposit(
            CreateVNPayDepositRequest request,
            HttpServletRequest servletRequest);

    VNPayIpnResponse processIpn(Map<String, String> params);

    VNPayReturnResponse processReturn(Map<String, String> params);

    PaymentSummaryResponse getPayment(String transactionRef);

    PageResponse<PaymentSummaryResponse> getMyPayments(
            PaymentStatus status,
            Pageable pageable);

    PageResponse<AdminPaymentResponse> getPaymentsForAdmin(
            Long userId,
            PaymentStatus status,
            PaymentMethod method,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    AdminPaymentResponse getPaymentForAdmin(String transactionRef);
}
