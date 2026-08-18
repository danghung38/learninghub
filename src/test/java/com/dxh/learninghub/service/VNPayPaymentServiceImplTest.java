package com.dxh.learninghub.service;

import com.dxh.learninghub.configuration.VNPayProperties;
import com.dxh.learninghub.dto.request.CreateVNPayDepositRequest;
import com.dxh.learninghub.dto.response.VNPayIpnResponse;
import com.dxh.learninghub.dto.response.VNPayPaymentResponse;
import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PaymentStatus;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.PointTransactionMapper;
import com.dxh.learninghub.repo.PaymentRepository;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.VNPayPaymentServiceImpl;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import com.dxh.learninghub.utils.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayPaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PointTransactionRepository pointTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock VNPayProperties properties;
    @Mock VNPayUtil vnPayUtil;
    @InjectMocks VNPayPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getAmountPerPoint()).thenReturn(1_000L);
        lenient().when(properties.getExpireMinutes()).thenReturn(15L);
        lenient().when(properties.getTmnCode()).thenReturn("LEARNING");
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
        when(vnPayUtil.buildPaymentUrl(any(Payment.class), eq("203.0.113.7"), any()))
                .thenReturn("https://sandbox.vnpay.vn/pay");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        VNPayPaymentResponse response = service.createDeposit(
                new CreateVNPayDepositRequest(100_000L), servletRequest);

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
                new CreateVNPayDepositRequest(100_500L), new MockHttpServletRequest()))
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
