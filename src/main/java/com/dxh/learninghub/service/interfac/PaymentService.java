package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.CreateDepositRequest;
import com.dxh.learninghub.dto.payment.VNPayIpnResponse;
import com.dxh.learninghub.dto.payment.PaymentCheckoutResponse;
import com.dxh.learninghub.dto.payment.VNPayReturnResponse;
import com.dxh.learninghub.dto.payment.PayOSWebhookResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.admin.AdminPaymentResponse;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

import vn.payos.model.webhooks.Webhook;

public interface PaymentService {
    PaymentCheckoutResponse createDeposit(
            CreateDepositRequest request,
            HttpServletRequest servletRequest);

    VNPayIpnResponse processIpn(Map<String, String> params);

    /** Receive and acknowledge the signed, server-to-server payOS webhook. */
    PayOSWebhookResponse processPayOSWebhook(Webhook webhook);

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
