package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.CreateWithdrawalRequest;
import com.dxh.learninghub.dto.request.RejectWithdrawalRequest;
import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.WithdrawalResponse;
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
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.interfac.WithdrawalService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import com.dxh.learninghub.utils.CurrentUserProvider;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {

    WithdrawalRepository withdrawalRepository;
    BankAccountRepository bankAccountRepository;
    PointTransactionRepository pointTransactionRepository;
    UserRepository userRepository;
    WithdrawalMapper withdrawalMapper;
    CurrentUserProvider currentUserProvider;
    NotificationService notificationService;
    AwsS3Service awsS3Service;

    @NonFinal
    @Value("${withdrawal.amount-per-point:800}")
    BigDecimal amountPerPoint;

    @PostConstruct
    void validateConfiguration() {
        if (amountPerPoint.signum() <= 0) {
            throw new IllegalArgumentException("withdrawal.amount-per-point must be positive");
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public WithdrawalResponse create(CreateWithdrawalRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();
        User teacher = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        BankAccount bankAccount = bankAccountRepository
                .findByIdAndTeacherId(request.bankAccountId(), teacher.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_EXISTED));
        if (!Boolean.TRUE.equals(bankAccount.getActive())) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_INACTIVE);
        }

        long currentPoints = teacher.getPoints() == null ? 0L : teacher.getPoints();
        if (currentPoints < request.points()) {
            throw new AppException(ErrorCode.WITHDRAWAL_INSUFFICIENT_POINTS);
        }

        teacher.setPoints(currentPoints - request.points());
        Withdrawal withdrawal = withdrawalRepository.save(
                Withdrawal.builder()
                        .teacher(teacher)
                        .bankAccount(bankAccount)
                        .bankName(bankAccount.getBankName())
                        .accountNumber(bankAccount.getAccountNumber())
                        .accountHolder(bankAccount.getAccountHolder())
                        .points(request.points())
                        .amount(amountPerPoint.multiply(BigDecimal.valueOf(request.points())))
                        .status(WithdrawalStatus.PENDING)
                        .build());

        pointTransactionRepository.save(
                PointTransaction.builder()
                        .user(teacher)
                        .points(request.points())
                        .transactionType(PointTransactionType.WITHDRAW)
                        .description("Withdrawal request #" + withdrawal.getId())
                        .build());

        User admin = userRepository.findByRoles_Name(RoleEnum.ADMIN.name())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        notificationService.createNotification(
                admin,
                teacher,
                "New withdrawal request",
                teacher.getFullName() + " requested a withdrawal of "
                        + request.points() + " points",
                "/admin/withdrawals"
        );

        return withdrawalMapper.toTeacherResponse(withdrawal);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PageResponse<WithdrawalResponse> getMyWithdrawals(Pageable pageable) {
        User teacher = currentUserProvider.getCurrentUser();
        Page<Withdrawal> page = withdrawalRepository
                .findAllByTeacherIdOrderByCreatedAtDesc(teacher.getId(), pageable);

        return PageResponse.<WithdrawalResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(withdrawalMapper::toTeacherResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<AdminWithdrawalResponse> getAllWithdrawals(
            WithdrawalStatus status,
            Pageable pageable) {
        Page<Withdrawal> page = withdrawalRepository.findAllByStatus(status, pageable);

        return PageResponse.<AdminWithdrawalResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(withdrawalMapper::toAdminResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminWithdrawalResponse getWithdrawalById(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_NOT_EXISTED));
        return withdrawalMapper.toAdminResponse(withdrawal);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminWithdrawalResponse markAsPaid(
            Long withdrawalId,
            MultipartFile file) {
        Withdrawal withdrawal = findPendingWithdrawalForUpdate(withdrawalId);
        User admin = currentUserProvider.getCurrentUser();
        String paymentProofObjectKey = awsS3Service.uploadFile(file,
                "withdrawals/" + admin.getId() + "/payment-proofs",
                UploadPolicy.PAYMENT_PROOF);

        withdrawal.setStatus(WithdrawalStatus.PAID);
        withdrawal.setPaymentProofUrl(paymentProofObjectKey);
        withdrawal.setRejectionReason(null);

        notificationService.createNotification(
                withdrawal.getTeacher(),
                null,
                "Withdrawal paid",
                "Your withdrawal request #" + withdrawal.getId() + " has been paid",
                "/teacher/withdrawals");

        return withdrawalMapper.toAdminResponse(withdrawal);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminWithdrawalResponse reject(
            Long withdrawalId,
            RejectWithdrawalRequest request) {
        Withdrawal withdrawal = findPendingWithdrawalForUpdate(withdrawalId);
        User teacher = userRepository.findByIdForUpdate(withdrawal.getTeacher().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        long currentPoints = teacher.getPoints() == null ? 0L : teacher.getPoints();
        teacher.setPoints(currentPoints + withdrawal.getPoints());
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(request.reason().trim());

        pointTransactionRepository.save(
                PointTransaction.builder()
                        .user(teacher)
                        .points(withdrawal.getPoints())
                        .transactionType(PointTransactionType.WITHDRAW_REFUND)
                        .description("Refund for rejected withdrawal #" + withdrawal.getId())
                        .build());

        notificationService.createNotification(
                teacher,
                null,
                "Withdrawal rejected",
                "Your withdrawal request #" + withdrawal.getId()
                        + " was rejected: " + request.reason().trim(),
                "/teacher/withdrawals");

        return withdrawalMapper.toAdminResponse(withdrawal);
    }

    private Withdrawal findPendingWithdrawalForUpdate(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findByIdForUpdate(withdrawalId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAWAL_NOT_EXISTED));
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new AppException(ErrorCode.WITHDRAWAL_NOT_PENDING);
        }
        return withdrawal;
    }

}
