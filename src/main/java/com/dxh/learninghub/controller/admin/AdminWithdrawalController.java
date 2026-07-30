package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.RejectWithdrawalRequest;
import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.enums.WithdrawalStatus;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/admin/withdrawals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Withdrawals", description = "APIs for administrators to process teacher withdrawals")
public class AdminWithdrawalController {

    WithdrawalService withdrawalService;

    @Operation(summary = "Get withdrawal requests", description = "Filter and paginate withdrawal requests")
    @GetMapping
    public ApiResponse<PageResponse<AdminWithdrawalResponse>> getAll(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) WithdrawalStatus status) {
        Pageable pageable = PageUtil.createPageable(pageNo, pageSize, "createdAt:desc");
        return ApiResponse.<PageResponse<AdminWithdrawalResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get withdrawals successfully")
                .result(withdrawalService.getAllWithdrawals(status, pageable))
                .build();
    }

    @Operation(summary = "Get withdrawal by ID", description = "Return one withdrawal request for fast administrative review")
    @GetMapping("/{id}")
    public ApiResponse<AdminWithdrawalResponse> getById(@PathVariable Long id) {
        return ApiResponse.<AdminWithdrawalResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get withdrawal successfully")
                .result(withdrawalService.getWithdrawalById(id))
                .build();
    }

    @Operation(summary = "Mark withdrawal as paid", description = "Mark a pending withdrawal as paid and upload payment proof")
    @PutMapping(value = "/{id}/paid", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminWithdrawalResponse> markAsPaid(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<AdminWithdrawalResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Mark withdrawal as paid successfully")
                .result(withdrawalService.markAsPaid(id, file))
                .build();
    }

    @Operation(summary = "Reject a withdrawal", description = "Reject a pending withdrawal and refund its points")
    @PutMapping("/{id}/reject")
    public ApiResponse<AdminWithdrawalResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectWithdrawalRequest request) {
        return ApiResponse.<AdminWithdrawalResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Reject withdrawal successfully")
                .result(withdrawalService.reject(id, request))
                .build();
    }
}
