package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.CreateBankAccountRequest;
import com.dxh.learninghub.dto.request.UpdateBankAccountRequest;
import com.dxh.learninghub.dto.response.BankAccountResponse;
import com.dxh.learninghub.entity.BankAccount;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.BankAccountMapper;
import com.dxh.learninghub.repo.BankAccountRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.interfac.BankAccountService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BankAccountServiceImpl implements BankAccountService {

    BankAccountRepository bankAccountRepository;
    BankAccountMapper bankAccountMapper;
    UserRepository userRepository;
    CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public BankAccountResponse create(CreateBankAccountRequest request) {
        User teacher = getLockedCurrentTeacher();
        List<BankAccount> activeAccounts =
                bankAccountRepository.findAllByTeacherIdAndActiveTrue(teacher.getId());

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault())
                || activeAccounts.isEmpty();
        if (makeDefault) {
            activeAccounts.forEach(account -> account.setIsDefault(false));
        }

        BankAccount bankAccount = bankAccountMapper.toEntity(request);
        normalize(bankAccount);
        bankAccount.setTeacher(teacher);
        bankAccount.setActive(true);
        bankAccount.setIsDefault(makeDefault);

        return bankAccountMapper.toResponse(bankAccountRepository.save(bankAccount));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public BankAccountResponse update(
            Long bankAccountId,
            UpdateBankAccountRequest request) {
        User teacher = getLockedCurrentTeacher();
        BankAccount bankAccount = findOwnedAccount(bankAccountId, teacher.getId());
        validateActive(bankAccount);

        if (Boolean.TRUE.equals(request.isDefault())) {
            bankAccountRepository.findAllByTeacherIdAndActiveTrue(teacher.getId())
                    .stream()
                    .filter(account -> !account.getId().equals(bankAccountId))
                    .forEach(account -> account.setIsDefault(false));
        }

        bankAccountMapper.update(request, bankAccount);
        normalize(bankAccount);
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public void delete(Long bankAccountId) {
        User teacher = getLockedCurrentTeacher();
        BankAccount bankAccount = findOwnedAccount(bankAccountId, teacher.getId());
        validateActive(bankAccount);

        boolean wasDefault = Boolean.TRUE.equals(bankAccount.getIsDefault());
        bankAccount.setActive(false);
        bankAccount.setIsDefault(false);

        if (wasDefault) {
            bankAccountRepository.findAllByTeacherIdAndActiveTrue(teacher.getId())
                    .stream()
                    .filter(account -> !account.getId().equals(bankAccountId))
                    .min(Comparator.comparing(BankAccount::getCreatedAt))
                    .ifPresent(account -> account.setIsDefault(true));
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public List<BankAccountResponse> getMyBankAccounts() {
        User teacher = currentUserProvider.getCurrentUser();
        return bankAccountRepository.findAllByTeacherIdAndActiveTrue(teacher.getId())
                .stream()
                .sorted(Comparator
                        .comparing(BankAccount::getIsDefault, Comparator.reverseOrder())
                        .thenComparing(BankAccount::getCreatedAt, Comparator.reverseOrder()))
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    private User getLockedCurrentTeacher() {
        User currentUser = currentUserProvider.getCurrentUser();
        return userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private BankAccount findOwnedAccount(Long bankAccountId, Long teacherId) {
        return bankAccountRepository.findByIdAndTeacherId(bankAccountId, teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_EXISTED));
    }

    private void validateActive(BankAccount bankAccount) {
        if (!Boolean.TRUE.equals(bankAccount.getActive())) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_INACTIVE);
        }
    }

    private void normalize(BankAccount bankAccount) {
        bankAccount.setBankName(bankAccount.getBankName().trim());
        bankAccount.setAccountNumber(bankAccount.getAccountNumber().trim());
        bankAccount.setAccountHolder(bankAccount.getAccountHolder().trim());
    }
}
