package com.dxh.learninghub.service.payment;

import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPayPaymentStrategy implements PaymentGatewayStrategy {

    VNPayUtil vnPayUtil;

    @Override
    public PaymentMethod paymentMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public String createPaymentUrl(
            Payment payment,
            HttpServletRequest request,
            LocalDateTime createdAt) {
        return vnPayUtil.buildPaymentUrl(payment, extractIpAddress(request), createdAt);
    }

    private static String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
