package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.service.interfac.admin.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Reviews", description = "APIs for administrators to moderate course reviews")
public class AdminReviewController {

    AdminReviewService adminReviewService;

    @Operation(summary = "Delete a review", description = "Permanently delete a review or reply")
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        adminReviewService.delete(reviewId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete review successfully")
                .build();
    }
}
