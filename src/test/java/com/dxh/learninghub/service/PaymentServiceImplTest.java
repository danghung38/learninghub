package com.dxh.learninghub.service;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.request.CreateDepositRequest;
import com.dxh.learninghub.dto.payment.VNPayIpnResponse;
import com.dxh.learninghub.dto.payment.PaymentCheckoutResponse;
import com.dxh.learninghub.dto.payment.PayOSWebhookResponse;
import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.PointTransactionMapper;
import com.dxh.learninghub.repo.PaymentRepository;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.PaymentServiceImpl;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.payment.PaymentGatewayStrategy;
import com.dxh.learninghub.service.payment.WebhookPaymentGatewayStrategy;
import com.dxh.learninghub.utils.CurrentUserProvider;
import com.dxh.learninghub.utils.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PointTransactionRepository pointTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock PointTransactionMapper pointTransactionMapper;
    @Mock NotificationService notificationService;
    @Mock VNPayProperties properties;
    @Mock VNPayUtil vnPayUtil;
    @Mock PaymentGatewayStrategy paymentGatewayStrategy;
    @Mock WebhookPaymentGatewayStrategy payOSGatewayStrategy;
    PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                paymentRepository,
                pointTransactionRepository,
                userRepository,
                currentUserProvider,
                pointTransactionMapper,
                notificationService,
                properties,
                vnPayUtil,
                List.of(paymentGatewayStrategy, payOSGatewayStrategy));
        lenient().when(properties.getAmountPerPoint()).thenReturn(1_000L);
        lenient().when(properties.getExpireMinutes()).thenReturn(15L);
        lenient().when(properties.getTmnCode()).thenReturn("LEARNING");
        lenient().when(paymentGatewayStrategy.paymentMethod()).thenReturn(PaymentMethod.VNPAY);
        lenient().when(payOSGatewayStrategy.paymentMethod()).thenReturn(PaymentMethod.PAYOS);
    }

    @Test
    void createDeposit_persistsPendingPaymentAndBuildsGatewayUrl() {
        User user = user(5L, 20L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("203.0.113.7");
        when(paymentGatewayStrategy.createPaymentUrl(any(Payment.class), same(servletRequest), any()))
                .thenReturn("https://sandbox.vnpay.vn/pay");

        PaymentCheckoutResponse response = service.createDeposit(
                new CreateDepositRequest(100_000L, PaymentMethod.VNPAY),
                servletRequest);

        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(100_000L));
        assertThat(response.points()).isEqualTo(100L);
        assertThat(response.paymentUrl()).isEqualTo("https://sandbox.vnpay.vn/pay");
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getValue().getUser()).isSameAs(user);
    }

    @Test
    void createDeposit_rejectsAmountNotDivisibleByPointRate() {
        assertThatThrownBy(() -> service.createDeposit(
                new CreateDepositRequest(100_500L, PaymentMethod.VNPAY),
                new MockHttpServletRequest()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DEPOSIT_AMOUNT));
        verifyNoInteractions(paymentRepository, currentUserProvider, vnPayUtil);
    }

    @Test
    void processIpn_rejectsInvalidSignatureBeforeDatabaseLookup() {
        Map<String, String> params = validIpnParams();
        when(vnPayUtil.isValidSignature(params)).thenReturn(false);

        VNPayIpnResponse response = service.processIpn(params);

        assertThat(response.responseCode()).isEqualTo("97");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void processIpn_successCreditsPointsExactlyOnce() {
        User user = user(5L, 20L);
        Payment payment = Payment.builder().user(user).merchantTransactionRef("TXN-1")
                .amount(BigDecimal.valueOf(100_000L)).pointsReceived(100L)
                .status(PaymentStatus.PENDING).build();
        payment.setId(10L);
        Map<String, String> params = validIpnParams();
        params.put("vnp_PayDate", "20260817123045");
        when(vnPayUtil.isValidSignature(params)).thenReturn(true);
        when(paymentRepository.findByTransactionRefForUpdate("TXN-1")).thenReturn(Optional.of(payment));
        when(pointTransactionRepository.existsByPaymentId(10L)).thenReturn(false);
        when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));

        VNPayIpnResponse response = service.processIpn(params);

        assertThat(response.responseCode()).isEqualTo("00");
        assertThat(user.getPoints()).isEqualTo(120L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        ArgumentCaptor<PointTransaction> transaction = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getTransactionType()).isEqualTo(PointTransactionType.DEPOSIT);
        assertThat(transaction.getValue().getPayment()).isSameAs(payment);
    }

    @Test
    void processPayOSWebhook_acceptsUuidReferenceAndCreditsPointsExactlyOnce() {
        String gatewayReference = "2c9d02e5-316b-477c-8167-c52dd3963f57";
        User user = user(5L, 20L);
        Payment payment = Payment.builder()
                .user(user)
                .merchantTransactionRef("1788444557483")
                .paymentMethod(PaymentMethod.PAYOS)
                .amount(BigDecimal.valueOf(2_000L))
                .pointsReceived(2L)
                .status(PaymentStatus.PENDING)
                .build();
        payment.setId(10L);

        WebhookData data = WebhookData.builder()
                .orderCode(1_788_444_557_483L)
                .amount(2_000L)
                .description("Thành công")
                .accountNumber("123456789")
                .reference(gatewayReference)
                .transactionDateTime("2026-09-03 21:09:41")
                .currency("VND")
                .code("00")
                .desc("Thành công")
                .paymentLinkId("plink_123456789")      // <--- Giải quyết lỗi paymentLinkId null
                .counterAccountBankName("NCB")
                .counterAccountBankId("970419")
                .counterAccountName("NGUYEN VAN A")
                .counterAccountNumber("987654321")
                .virtualAccountName("LEARNINGHUB")
                .virtualAccountNumber("123456")
                .build();
        Webhook webhook = new Webhook("00", "success", true, data, "valid-signature");

        when(payOSGatewayStrategy.verifyWebhook(webhook)).thenReturn(data);
        when(paymentRepository.findByTransactionRefForUpdate("1788444557483"))
                .thenReturn(Optional.of(payment));
        when(pointTransactionRepository.existsByPaymentId(10L)).thenReturn(false);
        when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));

        PayOSWebhookResponse response = service.processPayOSWebhook(webhook);

        assertThat(response.code()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getGatewayTransactionNo()).isEqualTo(gatewayReference);
        assertThat(user.getPoints()).isEqualTo(22L);
        verify(pointTransactionRepository).save(any(PointTransaction.class));
    }
    @Test
    void cancelPayOSPayment_marksOnlyPendingOwnPaymentAsCanceled() {
        User user = user(5L, 20L);
        Payment payment = Payment.builder()
                .user(user)
                .merchantTransactionRef("1788439825436")
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentStatus.PENDING)
                .build();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(paymentRepository.findByTransactionRefForUpdate("1788439825436"))
                .thenReturn(Optional.of(payment));

        service.cancelPayOSPayment("1788439825436");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getResponseCode()).isEqualTo("CANCELLED");
        verify(notificationService).createNotification(
                same(user), isNull(), eq("Top-up Canceled"), anyString(), eq("/dashboard/wallet"));
    }

    private static Map<String, String> validIpnParams() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "hash");
        params.put("vnp_TmnCode", "LEARNING");
        params.put("vnp_TxnRef", "TXN-1");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", "1234");
        return params;
    }

    private static User user(Long id, Long points) {
        User user = User.builder().username("student").points(points).build();
        user.setId(id);
        return user;
    }
}
