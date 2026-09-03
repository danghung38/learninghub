package com.dxh.learninghub.service.payment;

import com.dxh.learninghub.configuration.PayOSProperties;
import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.ObjectProvider;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOSPaymentStrategy implements WebhookPaymentGatewayStrategy {

    static ZoneId PAYMENT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    ObjectProvider<PayOS> payOSProvider;
    PayOSProperties properties;

    @Override
    public PaymentMethod paymentMethod() {
        return PaymentMethod.PAYOS;
    }

    @Override
    public String createPaymentUrl(
            Payment payment,
            HttpServletRequest request,
            LocalDateTime createdAt) {
        try {
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(parseOrderCode(payment.getMerchantTransactionRef()))
                    .amount(toLong(payment.getAmount()))
                    .description("Point deposit " + payment.getPointsReceived())
                    .cancelUrl(redirectUrl(properties.getCancelUrl(), payment))
                    .returnUrl(redirectUrl(properties.getReturnUrl(), payment))
                    .expiredAt(payment.getExpiresAt() == null
                            ? null
                            : payment.getExpiresAt().atZone(PAYMENT_ZONE).toEpochSecond())
                    .build();

            CreatePaymentLinkResponse response = payOSProvider.getObject()
                    .paymentRequests()
                    .create(paymentRequest);
            if (response == null || !StringUtils.hasText(response.getCheckoutUrl())) {
                throw new IllegalStateException("payOS did not return a checkout URL");
            }
            return response.getCheckoutUrl();
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Unable to create payOS checkout link for {}", payment.getMerchantTransactionRef(), exception);
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    /** Verify the signature before the payment service mutates any state. */
    public WebhookData verifyWebhook(Webhook webhook) {
        if (webhook == null) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
        try {
            return payOSProvider.getObject().webhooks().verify(webhook);
        } catch (Exception exception) {
            log.warn("Rejected payOS webhook", exception);
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private static Long toLong(BigDecimal amount) {
        if (amount == null) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
        try {
            return amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
    }

    private static Long parseOrderCode(String transactionRef) {
        try {
            return Long.valueOf(transactionRef);
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private static String redirectUrl(String baseUrl, Payment payment) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .replaceQueryParam("transactionRef", payment.getMerchantTransactionRef())
                .build()
                .encode()
                .toUri();
        return uri.toString();
    }
}
