package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.request.PointAdjustmentRequest;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointAdjustmentResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.enums.PointTransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdminPointTransactionService {

    PointAdjustmentResponse creditPoints(Long userId, PointAdjustmentRequest request);

    PointAdjustmentResponse bonusPoints(Long userId, PointAdjustmentRequest request);

    PointAdjustmentResponse debitPoints(Long userId, PointAdjustmentRequest request);

    PageResponse<PointTransactionResponse> getAllTransactions(
            Long userId,
            PointTransactionType type,
            LocalDate from,
            LocalDate to,
            Pageable pageable);
}
