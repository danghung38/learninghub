package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.CreateWithdrawalRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.WithdrawalResponse;
import com.dxh.learninghub.service.interfac.WithdrawalService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;


@RestController
@RequestMapping("/teacher/withdrawals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teacher Withdrawals", description = "APIs for teachers to request and track withdrawals")
public class TeacherWithdrawalController {

    WithdrawalService withdrawalService;

    @Operation(summary = "Create a withdrawal", description = "Request a point withdrawal to one of the current teacher's bank accounts")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WithdrawalResponse> create(
            @Valid @RequestBody CreateWithdrawalRequest request) {
        return ApiResponse.<WithdrawalResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create withdrawal request successfully")
                .result(withdrawalService.create(request))
                .build();
    }

    @Operation(summary = "Get my withdrawals", description = "Return the current teacher's paginated withdrawal history")
    @GetMapping
    public ApiResponse<PageResponse<WithdrawalResponse>> getMyWithdrawals(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageUtil.createPageable(pageNo, pageSize, "createdAt:desc");
        return ApiResponse.<PageResponse<WithdrawalResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get withdrawal history successfully")
                .result(withdrawalService.getMyWithdrawals(pageable))
                .build();
    }
}
