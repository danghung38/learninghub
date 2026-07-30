package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.PointAdjustmentRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointAdjustmentResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.service.interfac.admin.AdminPointTransactionService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/point-transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Point Transactions", description = "APIs for administrators to inspect and adjust user points")
public class AdminPointTransactionController {
    AdminPointTransactionService adminPointTransactionService;

    @Operation(summary = "Get all point transactions", description = "Filter and paginate point transactions across all users")
    @GetMapping
    public ApiResponse<PageResponse<PointTransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PointTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "points", "transactionType", "createdAt", "updatedAt");
        return ApiResponse.<PageResponse<PointTransactionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get point transactions successfully")
                .result(adminPointTransactionService.getAllTransactions(userId, type, from, to, pageable))
                .build();
    }

    @Operation(summary = "Credit user points", description = "Credit a specified number of points to a user")
    @PostMapping("/users/{userId}/credit")
    public ApiResponse<PointAdjustmentResponse> creditPoints(
            @PathVariable Long userId,
            @Valid @RequestBody PointAdjustmentRequest request) {
        return ApiResponse.<PointAdjustmentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Credit points successfully")
                .result(adminPointTransactionService.creditPoints(userId, request))
                .build();
    }

    @Operation(summary = "Award bonus points", description = "Award bonus points to a selected user")
    @PostMapping("/users/{userId}/bonus")
    public ApiResponse<PointAdjustmentResponse> bonusPoints(
            @PathVariable Long userId,
            @Valid @RequestBody PointAdjustmentRequest request) {
        return ApiResponse.<PointAdjustmentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Award bonus points successfully")
                .result(adminPointTransactionService.bonusPoints(userId, request))
                .build();
    }

    @Operation(summary = "Debit user points", description = "Deduct a specified number of points from a user")
    @PostMapping("/users/{userId}/debit")
    public ApiResponse<PointAdjustmentResponse> debitPoints(
            @PathVariable Long userId,
            @Valid @RequestBody PointAdjustmentRequest request) {
        return ApiResponse.<PointAdjustmentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Debit points successfully")
                .result(adminPointTransactionService.debitPoints(userId, request))
                .build();
    }
}
