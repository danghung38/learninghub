package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.dto.response.UserPointBalanceResponse;
import com.dxh.learninghub.enums.PointTransactionType;
import org.springframework.data.domain.Pageable;


public interface UserPointService {
    UserPointBalanceResponse getMyPointBalance();

    PageResponse<PointTransactionResponse> getMyTransactions(
            PointTransactionType type,
            Pageable pageable);
}
