package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.request.CreateWithdrawalRequest;
import com.dxh.learninghub.dto.request.RejectWithdrawalRequest;
import com.dxh.learninghub.dto.response.WithdrawalResponse;
import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.entity.BankAccount;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.entity.Withdrawal;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.enums.WithdrawalStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.WithdrawalMapper;
import com.dxh.learninghub.repo.BankAccountRepository;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.repo.WithdrawalRepository;
import com.dxh.learninghub.service.impl.WithdrawalServiceImpl;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock WithdrawalRepository withdrawalRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock PointTransactionRepository pointTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock WithdrawalMapper withdrawalMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock NotificationService notificationService;
    @Mock AwsS3Service awsS3Service;
    @InjectMocks WithdrawalServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "amountPerPoint", BigDecimal.valueOf(800));
    }

    @Test
    void create_deductsPointsAndCreatesFinancialRecords() {
        User teacher = user(4L, 1_000L);
        User admin = user(1L, 0L);
        BankAccount account = BankAccount.builder().teacher(teacher).active(true)
                .bankName("VCB").accountNumber("123").accountHolder("TEACHER").build();
        account.setId(6L);
        Withdrawal saved = Withdrawal.builder().teacher(teacher).bankAccount(account).points(300L)
                .amount(BigDecimal.valueOf(240_000)).status(WithdrawalStatus.PENDING).build();
        saved.setId(11L);
        WithdrawalResponse response = new WithdrawalResponse(11L, 6L, saved.getAmount(), 300L,
                WithdrawalStatus.PENDING, "VCB", "123", "TEACHER", null, null, null);

        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(teacher));
        when(bankAccountRepository.findByIdAndTeacherId(6L, 4L)).thenReturn(Optional.of(account));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenReturn(saved);
        when(userRepository.findByRoles_Name(RoleEnum.ADMIN.name())).thenReturn(List.of(admin));
        when(withdrawalMapper.toTeacherResponse(saved)).thenReturn(response);

        assertThat(service.create(new CreateWithdrawalRequest(6L, 300L))).isSameAs(response);
        assertThat(teacher.getPoints()).isEqualTo(700L);
        ArgumentCaptor<PointTransaction> transaction = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getTransactionType()).isEqualTo(PointTransactionType.WITHDRAW);
        assertThat(transaction.getValue().getPoints()).isEqualTo(300L);
        verify(notificationService).createNotification(eq(admin), eq(teacher), anyString(), anyString(), eq("/admin/withdrawals"));
    }

    @Test
    void create_rejectsInsufficientPointsWithoutPersisting() {
        User teacher = user(4L, 99L);
        BankAccount account = BankAccount.builder().teacher(teacher).active(true).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(teacher));
        when(bankAccountRepository.findByIdAndTeacherId(6L, 4L)).thenReturn(Optional.of(account));

        assertError(() -> service.create(new CreateWithdrawalRequest(6L, 100L)),
                ErrorCode.WITHDRAWAL_INSUFFICIENT_POINTS);
        verifyNoInteractions(withdrawalRepository, pointTransactionRepository, notificationService);
    }

    @Test
    void create_rejectsInactiveBankAccount() {
        User teacher = user(4L, 1_000L);
        BankAccount account = BankAccount.builder().teacher(teacher).active(false).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(teacher);
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(teacher));
        when(bankAccountRepository.findByIdAndTeacherId(6L, 4L)).thenReturn(Optional.of(account));

        assertError(() -> service.create(new CreateWithdrawalRequest(6L, 100L)), ErrorCode.BANK_ACCOUNT_INACTIVE);
    }

    @Test
    void reject_refundsTeacherPointsAndRecordsRefund() {
        User teacher = user(4L, 200L);
        User admin = user(1L, 0L);
        Withdrawal withdrawal = Withdrawal.builder().teacher(teacher).points(300L).status(WithdrawalStatus.PENDING).build();
        withdrawal.setId(11L);
        AdminWithdrawalResponse response = mock(AdminWithdrawalResponse.class);
        when(withdrawalRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(withdrawal));
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(teacher));
        when(currentUserProvider.getCurrentUser()).thenReturn(admin);
        when(withdrawalMapper.toAdminResponse(withdrawal)).thenReturn(response);

        assertThat(service.reject(11L, new RejectWithdrawalRequest(" Sai tài khoản "))).isSameAs(response);
        assertThat(teacher.getPoints()).isEqualTo(500L);
        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(withdrawal.getRejectionReason()).isEqualTo("Sai tài khoản");
        ArgumentCaptor<PointTransaction> transaction = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getTransactionType()).isEqualTo(PointTransactionType.WITHDRAW_REFUND);
    }

    @Test
    void reject_rejectsAlreadyProcessedWithdrawal() {
        Withdrawal withdrawal = Withdrawal.builder().status(WithdrawalStatus.PAID).build();
        when(withdrawalRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(withdrawal));

        assertError(() -> service.reject(11L, new RejectWithdrawalRequest("reason")), ErrorCode.WITHDRAWAL_NOT_PENDING);
        verifyNoInteractions(pointTransactionRepository);
    }

    private static User user(Long id, Long points) {
        User user = User.builder().fullName("User").points(points).build();
        user.setId(id);
        return user;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
