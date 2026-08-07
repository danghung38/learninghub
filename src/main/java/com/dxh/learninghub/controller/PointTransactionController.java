package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.dto.response.UserPointBalanceResponse;
import com.dxh.learninghub.enums.PointTransactionType;
import com.dxh.learninghub.service.interfac.UserPointService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users/me/points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Point Transactions", description = "APIs for the current user's point balance and transactions")
public class PointTransactionController {
    UserPointService userPointService;

    @Operation(summary = "Get my point balance", description = "Return the current user's available point balance")
    @GetMapping
    public ApiResponse<UserPointBalanceResponse> getMyPointBalance() {
        return ApiResponse.<UserPointBalanceResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get point balance successfully")
                .result(userPointService.getMyPointBalance())
                .build();
    }

    @Operation(summary = "Get my point transactions", description = "Filter and paginate the current user's point transaction history")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<PointTransactionResponse>> getMyPointTransactions(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy,
            @RequestParam(required = false) PointTransactionType type) {
        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "points", "transactionType", "createdAt", "updatedAt");
        return ApiResponse.<PageResponse<PointTransactionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get point transactions successfully")
                .result(userPointService.getMyTransactions(type, pageable))
                .build();
    }

}
