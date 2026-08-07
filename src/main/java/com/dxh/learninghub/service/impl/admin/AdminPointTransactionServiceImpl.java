package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.dto.request.PointAdjustmentRequest;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointAdjustmentResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.PointTransactionMapper;
import com.dxh.learninghub.repo.PointTransactionRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.interfac.admin.AdminPointTransactionService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminPointTransactionServiceImpl implements AdminPointTransactionService {
    PointTransactionRepository pointTransactionRepository;
    PointTransactionMapper pointTransactionMapper;
    UserRepository userRepository;
    NotificationService notificationService;
    CurrentUserProvider currentUser;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PointAdjustmentResponse creditPoints(
            Long userId,
            PointAdjustmentRequest request) {
        return adjustPoints(userId, request, PointTransactionType.ADMIN_CREDIT);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PointAdjustmentResponse bonusPoints(
            Long userId,
            PointAdjustmentRequest request) {
        return adjustPoints(userId, request, PointTransactionType.BONUS);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PointAdjustmentResponse debitPoints(
            Long userId,
            PointAdjustmentRequest request) {
        return adjustPoints(userId, request, PointTransactionType.ADMIN_DEBIT);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<PointTransactionResponse> getAllTransactions(
            Long userId,
            PointTransactionType type,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay();
        Page<PointTransaction> page = pointTransactionRepository.findAllTransactions(
                userId, type, fromDate, toDate, pageable);

        return toPageResponse(page, pageable);
    }

    private PointAdjustmentResponse adjustPoints(
            Long userId,
            PointAdjustmentRequest request,
            PointTransactionType type) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        long currentPoints = user.getPoints() == null ? 0L : user.getPoints();
        long changedPoints;

        boolean addsPoints = type == PointTransactionType.ADMIN_CREDIT
                || type == PointTransactionType.BONUS;

        if (addsPoints) {
            changedPoints = request.points();
            user.setPoints(currentPoints + request.points());
        } else {
            if (currentPoints < request.points()) {
                throw new AppException(ErrorCode.INSUFFICIENT_USER_POINTS);
            }
            changedPoints = -request.points();
            user.setPoints(currentPoints - request.points());
        }

        PointTransaction transaction = pointTransactionRepository.save(
                PointTransaction.builder()
                        .user(user)
                        .points(request.points())
                        .transactionType(type)
                        .description(request.description().trim())
                        .build());

        String notificationTitle;
        String message;
        if (type == PointTransactionType.BONUS) {
            notificationTitle = "Bonus points received";
            message = "You received " + request.points() + " bonus points";
        } else if (type == PointTransactionType.ADMIN_CREDIT) {
            notificationTitle = "Point balance updated";
            message = "Admin credited " + request.points() + " points to your account";
        } else {
            notificationTitle = "Point balance updated";
            message = "Admin deducted " + request.points() + " points from your account";
        }

        notificationService.createNotification(
                user,
                currentUser.getCurrentUser(),
                notificationTitle,
                message,
                "/dashboard/wallet");

        return PointAdjustmentResponse.builder()
                .userId(user.getId())
                .transactionId(transaction.getId())
                .changedPoints(changedPoints)
                .currentPoints(user.getPoints())
                .transactionType(type)
                .build();
    }

    private PageResponse<PointTransactionResponse> toPageResponse(
            Page<PointTransaction> page,
            Pageable pageable) {
        return PageResponse.<PointTransactionResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(pointTransactionMapper::toResponse).toList())
                .build();
    }
}
