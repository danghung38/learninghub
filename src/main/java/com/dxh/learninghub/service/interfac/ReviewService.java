package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.ReviewRequest;
import com.dxh.learninghub.dto.request.ReviewReplyRequest;
import com.dxh.learninghub.dto.request.ReviewUpdateRequest;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.RatingSummaryResponse;
import com.dxh.learninghub.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;


public interface ReviewService {
    ReviewResponse create(Long courseId, ReviewRequest request);

    ReviewResponse reply(Long reviewId, ReviewReplyRequest request);

    PageResponse<ReviewResponse> getByCourse(Long courseId, Pageable pageable);

    ReviewResponse update(Long reviewId, ReviewUpdateRequest request);

    void deleteOwnReview(Long reviewId);

    RatingSummaryResponse getRatingSummary(Long courseId);
}
