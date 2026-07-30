package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.CreateWithdrawalRequest;
import com.dxh.learninghub.dto.request.RejectWithdrawalRequest;
import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.WithdrawalResponse;
import com.dxh.learninghub.enums.WithdrawalStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


public interface WithdrawalService {

    WithdrawalResponse create(CreateWithdrawalRequest request);

    PageResponse<WithdrawalResponse> getMyWithdrawals(Pageable pageable);

    PageResponse<AdminWithdrawalResponse> getAllWithdrawals(
            WithdrawalStatus status,
            Pageable pageable);

    AdminWithdrawalResponse getWithdrawalById(Long withdrawalId);

    AdminWithdrawalResponse markAsPaid(
            Long withdrawalId,
            MultipartFile file);

    AdminWithdrawalResponse reject(
            Long withdrawalId,
            RejectWithdrawalRequest request);

}
