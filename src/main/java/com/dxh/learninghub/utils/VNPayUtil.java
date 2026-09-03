package com.dxh.learninghub.utils;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VNPayUtil {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayProperties properties;

    public String buildPaymentUrl(Payment payment, String ipAddress, LocalDateTime createdAt) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", properties.getCommand());
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", toVNPayAmount(payment.getAmount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getMerchantTransactionRef());
        params.put("vnp_OrderInfo", "Point deposit LearningHub "
                + payment.getMerchantTransactionRef());
        params.put("vnp_OrderType", properties.getOrderType());
        params.put("vnp_Locale", properties.getLocale());
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createdAt.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", payment.getExpiresAt().format(VNPAY_DATE_FORMAT));

        String query = buildCanonicalQuery(params);
        String secureHash = hmacSHA512(properties.getHashSecret(), query);
        return properties.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean isValidSignature(Map<String, String> responseParams) {
        if (responseParams == null) {
            return false;
        }
        String receivedHash = responseParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = responseParams.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("vnp_"))
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash"))
                .filter(entry -> !entry.getKey().equals("vnp_SecureHashType"))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new));

        String expectedHash = hmacSHA512(
                properties.getHashSecret(), buildCanonicalQuery(signedParams));
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                receivedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    static String buildCanonicalQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot generate VNPAY signature", exception);
        }
    }

    private static String toVNPayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100L))
                .toBigIntegerExact()
                .toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
