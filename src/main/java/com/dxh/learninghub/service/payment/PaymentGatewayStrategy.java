package com.dxh.learninghub.service.payment;

import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * Provider-specific part of the deposit flow. Payment persistence, idempotency
 * and point crediting remain in {@code PaymentServiceImpl}.
 */
public interface PaymentGatewayStrategy {

    PaymentMethod paymentMethod();

    String createPaymentUrl(Payment payment, HttpServletRequest request, LocalDateTime createdAt);
}
