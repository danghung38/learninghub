package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.CreateBankAccountRequest;
import com.dxh.learninghub.dto.request.UpdateBankAccountRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.BankAccountResponse;
import com.dxh.learninghub.service.interfac.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/teacher/bank-accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teacher Bank Accounts", description = "APIs for teachers to manage withdrawal bank accounts")
public class TeacherBankAccountController {

    BankAccountService bankAccountService;

    @Operation(summary = "Add a bank account", description = "Add an active withdrawal bank account for the current teacher")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BankAccountResponse> create(
            @Valid @RequestBody CreateBankAccountRequest request) {
        return ApiResponse.<BankAccountResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create bank account successfully")
                .result(bankAccountService.create(request))
                .build();
    }

    @Operation(summary = "Update a bank account", description = "Update a bank account owned by the current teacher")
    @PatchMapping("/{id}")
    public ApiResponse<BankAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBankAccountRequest request) {
        return ApiResponse.<BankAccountResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update bank account successfully")
                .result(bankAccountService.update(id, request))
                .build();
    }

    @Operation(summary = "Delete a bank account", description = "Deactivate a bank account owned by the current teacher")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete bank account successfully")
                .build();
    }

    @Operation(summary = "Get my bank accounts", description = "Return the current teacher's active bank accounts")
    @GetMapping
    public ApiResponse<List<BankAccountResponse>> getMyBankAccounts() {
        return ApiResponse.<List<BankAccountResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get bank accounts successfully")
                .result(bankAccountService.getMyBankAccounts())
                .build();
    }
}
