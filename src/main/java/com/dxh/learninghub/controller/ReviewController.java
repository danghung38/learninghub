package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.ReviewRequest;
import com.dxh.learninghub.dto.request.ReviewReplyRequest;
import com.dxh.learninghub.dto.request.ReviewUpdateRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.RatingSummaryResponse;
import com.dxh.learninghub.dto.response.ReviewResponse;
import com.dxh.learninghub.service.interfac.ReviewService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Reviews", description = "APIs for course reviews, replies, and rating summaries")
public class ReviewController {

    ReviewService reviewService;

    @Operation(summary = "Create a course review", description = "Review an enrolled course as the current user")
    @PostMapping("/courses/{courseId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create review successfully")
                .result(reviewService.create(courseId, request))
                .build();
    }

    @Operation(summary = "Get course reviews", description = "Return paginated reviews and replies for an approved course")
    @GetMapping("/courses/{courseId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy) {
        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "rating", "createdAt", "updatedAt");
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get reviews successfully")
                .result(reviewService.getByCourse(courseId, pageable))
                .build();
    }

    @Operation(summary = "Reply to a review", description = "Reply to a course review as its teacher or an administrator")
    @PostMapping("/reviews/{reviewId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> reply(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Reply to review successfully")
                .result(reviewService.reply(reviewId, request))
                .build();
    }

    @Operation(summary = "Get rating summary", description = "Return rating statistics for an approved course")
    @GetMapping("/courses/{courseId}/rating-summary")
    public ApiResponse<RatingSummaryResponse> getRatingSummary(@PathVariable Long courseId) {
        return ApiResponse.<RatingSummaryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get rating summary successfully")
                .result(reviewService.getRatingSummary(courseId))
                .build();
    }

    @Operation(summary = "Update my review", description = "Update a course review owned by the current user")
    @PatchMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> update(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update review successfully")
                .result(reviewService.update(reviewId, request))
                .build();
    }

    @Operation(summary = "Delete my review", description = "Delete a course review owned by the current user")
    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        reviewService.deleteOwnReview(reviewId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete review successfully")
                .build();
    }
}
