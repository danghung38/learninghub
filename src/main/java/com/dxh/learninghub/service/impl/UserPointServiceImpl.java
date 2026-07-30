package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.dto.response.UserPointBalanceResponse;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.PointTransactionMapper;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.service.interfac.UserPointService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPointServiceImpl implements UserPointService {
    PointTransactionRepository pointTransactionRepository;
    PointTransactionMapper pointTransactionMapper;
    CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public UserPointBalanceResponse getMyPointBalance() {
        User user = currentUserProvider.getCurrentUser();
        return UserPointBalanceResponse.builder()
                .points(user.getPoints())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PointTransactionResponse getMyTransaction(Long transactionId) {
        User user = currentUserProvider.getCurrentUser();
        PointTransaction transaction = pointTransactionRepository
                .findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.POINT_TRANSACTION_NOT_EXISTED));
        return pointTransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PointTransactionResponse> getMyTransactions(
            PointTransactionType type,
            Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        Page<PointTransaction> page =
                pointTransactionRepository.findMyTransactions(user, type, pageable);

        return PageResponse.<PointTransactionResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(pointTransactionMapper::toResponse).toList())
                .build();
    }
}
