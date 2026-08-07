package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.request.CreateVNPayDepositRequest;
import com.dxh.learninghub.dto.response.VNPayIpnResponse;
import com.dxh.learninghub.dto.response.VNPayPaymentResponse;
import com.dxh.learninghub.dto.response.VNPayReturnResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.admin.AdminPaymentResponse;
import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.PointTransactionMapper;
import com.dxh.learninghub.repo.PaymentRepository;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.interfac.VNPayPaymentService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import com.dxh.learninghub.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VNPayPaymentServiceImpl implements VNPayPaymentService {

    private static final DateTimeFormatter TRANSACTION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    PaymentRepository paymentRepository;
    PointTransactionRepository pointTransactionRepository;
    UserRepository userRepository;
    CurrentUserProvider currentUserProvider;
    PointTransactionMapper pointTransactionMapper;
    NotificationService notificationService;
    VNPayProperties properties;
    VNPayUtil vnPayUtil;

    @Override
    @Transactional
    public VNPayPaymentResponse createDeposit(
            CreateVNPayDepositRequest request,
            HttpServletRequest servletRequest) {
        if (request.amount() % properties.getAmountPerPoint() != 0) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }

        User user = currentUserProvider.getCurrentUser();
        LocalDateTime now = LocalDateTime.now(VNPAY_ZONE);
        long points = request.amount() / properties.getAmountPerPoint();
        Payment payment = paymentRepository.save(
                Payment.builder()
                        .user(user)
                        .merchantTransactionRef(newTransactionRef())
                        .paymentMethod(PaymentMethod.VNPAY)
                        .status(PaymentStatus.PENDING)
                        .amount(BigDecimal.valueOf(request.amount()))
                        .pointsReceived(points)
                        .expiresAt(now.plusMinutes(properties.getExpireMinutes()))
                        .build());

        String paymentUrl = vnPayUtil.buildPaymentUrl(payment, extractIpAddress(servletRequest), now);
        return new VNPayPaymentResponse(
                payment.getMerchantTransactionRef(),
                payment.getAmount(),
                payment.getPointsReceived(),
                paymentUrl,
                payment.getExpiresAt());
    }

    @Override
    @Transactional
    public VNPayIpnResponse processIpn(Map<String, String> params) {
        if (!hasRequiredCallbackData(params)) {
            return ipn("99", "Invalid request");
        }
        if (!vnPayUtil.isValidSignature(params)) {
            log.warn("Rejected VNPAY IPN because signature is invalid");
            return ipn("97", "Invalid signature");
        }
        if (!properties.getTmnCode().equals(params.get("vnp_TmnCode"))) {
            return ipn("99", "Invalid terminal code");
        }

        Payment payment = paymentRepository
                .findByTransactionRefForUpdate(params.get("vnp_TxnRef"))
                .orElse(null);
        if (payment == null) {
            return ipn("01", "Order not found");
        }
        if (!isExpectedAmount(payment, params.get("vnp_Amount"))) {
            return ipn("04", "Invalid amount");
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED
                || pointTransactionRepository.existsByPaymentId(payment.getId())) {
            return ipn("02", "Order already confirmed");
        }

        payment.setResponseCode(params.get("vnp_ResponseCode"));
        // Sửa đoạn gán gatewayTransactionNo trong processIpn():
        String gatewayTxnNo = params.get("vnp_TransactionNo");
        payment.setGatewayTransactionNo(normalizeTransactionNo(gatewayTxnNo));
        payment.setBankCode(blankToNull(params.get("vnp_BankCode")));

        boolean successful = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        if (!successful) {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                return ipn("02", "Order already confirmed");
            }
            payment.setStatus(toFailureStatus(params.get("vnp_ResponseCode")));
            notificationService.createNotification(
                    payment.getUser(),
                    null,
                    "Top-up Failed",
                    failureNotificationMessage(payment),
                    "/dashboard/wallet"
                    // for fe
            );
            log.info("VNPAY deposit {} failed with response code {}",
                    payment.getMerchantTransactionRef(), payment.getResponseCode());
            return ipn("00", "Confirm success");
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.EXPIRED) {
            return ipn("02", "Order already confirmed");
        }

        User user = userRepository.findByIdForUpdate(payment.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        long currentPoints = user.getPoints() == null ? 0L : user.getPoints();
        user.setPoints(Math.addExact(currentPoints, payment.getPointsReceived()));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(parsePayDate(params.get("vnp_PayDate")));

        pointTransactionRepository.save(
                PointTransaction.builder()
                        .user(user)
                        .payment(payment)
                        .points(payment.getPointsReceived())
                        .transactionType(PointTransactionType.DEPOSIT)
                        .description("VNPAY deposit "
                                + payment.getMerchantTransactionRef())
                        .build());

        notificationService.createNotification(
                user,
                null,
                "Top-up Successful",
                "Transaction " + payment.getMerchantTransactionRef()
                        + " has added " + payment.getPointsReceived()
                        + " points to your account",
                "/dashboard/wallet"
        );

        log.info("Confirmed VNPAY deposit {} and credited {} points to user {}",
                payment.getMerchantTransactionRef(), payment.getPointsReceived(), user.getId());
        return ipn("00", "Confirm success");
    }

    @Override
    public VNPayReturnResponse processReturn(Map<String, String> params) {
        String transactionRef = params == null ? null : params.get("vnp_TxnRef");
        boolean signatureValid = hasRequiredReturnData(params)
                && vnPayUtil.isValidSignature(params)
                && properties.getTmnCode().equals(params.get("vnp_TmnCode"));
        return new VNPayReturnResponse(transactionRef, signatureValid);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPayment(String transactionRef) {
        User user = currentUserProvider.getCurrentUser();
        Payment payment = paymentRepository
                .findByMerchantTransactionRefAndUserId(transactionRef, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_EXISTED));
        return pointTransactionMapper.toPaymentSummary(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> getMyPayments(
            PaymentStatus status,
            Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        Page<Payment> page = paymentRepository.findPayments(
                user.getId(), status, PaymentMethod.VNPAY, null, null, pageable);
        return toPageResponse(
                page.map(pointTransactionMapper::toPaymentSummary), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<AdminPaymentResponse> getPaymentsForAdmin(
            Long userId,
            PaymentStatus status,
            PaymentMethod method,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) throw new AppException(ErrorCode.INVALID_DATE_RANGE);

        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay();
        Page<Payment> page = paymentRepository.findPayments(userId, status, method, fromDate, toDate, pageable);
        return toPageResponse(page.map(this::toAdminResponse), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminPaymentResponse getPaymentForAdmin(String transactionRef) {
        Payment payment = paymentRepository
                .findByMerchantTransactionRef(transactionRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_EXISTED));
        return toAdminResponse(payment);
    }

    private static boolean hasRequiredCallbackData(Map<String, String> params) {
        return params != null
                && StringUtils.hasText(params.get("vnp_SecureHash"))
                && StringUtils.hasText(params.get("vnp_TmnCode"))
                && StringUtils.hasText(params.get("vnp_TxnRef"))
                && StringUtils.hasText(params.get("vnp_Amount"))
                && StringUtils.hasText(params.get("vnp_ResponseCode"))
                && StringUtils.hasText(params.get("vnp_TransactionStatus"));
    }

    private static boolean hasRequiredReturnData(Map<String, String> params) {
        return params != null
                && StringUtils.hasText(params.get("vnp_SecureHash"))
                && StringUtils.hasText(params.get("vnp_TmnCode"))
                && StringUtils.hasText(params.get("vnp_TxnRef"));
    }

    private static boolean isExpectedAmount(Payment payment, String callbackAmount) {
        try {
            BigDecimal expected = payment.getAmount().multiply(BigDecimal.valueOf(100L));
            return expected.compareTo(new BigDecimal(callbackAmount)) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static PaymentStatus toFailureStatus(String responseCode) {
        if ("24".equals(responseCode)) {
            return PaymentStatus.CANCELED;
        }
        if ("11".equals(responseCode)) {
            return PaymentStatus.EXPIRED;
        }
        return PaymentStatus.FAILED;
    }

    private static String failureNotificationMessage(Payment payment) {
        String reason = switch (payment.getStatus()) {
            case CANCELED -> "has been canceled";
            case EXPIRED -> "has expired";
            default -> "could not be completed";
        };
        return "Transaction " + payment.getMerchantTransactionRef() + " " + reason
                + ". Your account has not been charged or credited points";
    }

    private AdminPaymentResponse toAdminResponse(Payment payment) {
        User user = payment.getUser();
        return new AdminPaymentResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                pointTransactionMapper.toPaymentSummary(payment));
    }

    private static <T> PageResponse<T> toPageResponse(
            Page<T> page,
            Pageable pageable) {
        return PageResponse.<T>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.getContent())
                .build();
    }

    private static LocalDateTime parsePayDate(String payDate) {
        if (!StringUtils.hasText(payDate)) {
            log.warn("VNPAY callback does not contain vnp_PayDate");
            return null;
        }
        try {
            return LocalDateTime.parse(payDate, TRANSACTION_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            log.warn("VNPAY callback contains invalid vnp_PayDate: {}", payDate);
            return null;
        }
    }

    private String normalizeTransactionNo(String txnNo) {
        if (!StringUtils.hasText(txnNo) || "0".equals(txnNo.trim())) {
            return null; // Trả về NULL để DB không bị tính trùng lặp Unique
        }
        return txnNo.trim();
    }

    private static String newTransactionRef() {
        String timestamp = LocalDateTime.now(VNPAY_ZONE)
                .format(TRANSACTION_TIME_FORMAT);
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
        return "DEP" + timestamp + randomPart;
    }

    private static String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private static VNPayIpnResponse ipn(String code, String message) {
        return new VNPayIpnResponse(code, message);
    }
}
