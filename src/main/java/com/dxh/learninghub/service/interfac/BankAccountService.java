package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.CreateBankAccountRequest;
import com.dxh.learninghub.dto.request.UpdateBankAccountRequest;
import com.dxh.learninghub.dto.response.BankAccountResponse;

import java.util.List;

public interface BankAccountService {

    BankAccountResponse create(CreateBankAccountRequest request);

    BankAccountResponse update(Long bankAccountId, UpdateBankAccountRequest request);

    void delete(Long bankAccountId);

    List<BankAccountResponse> getMyBankAccounts();
}
