package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.entity.Review;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.ReviewRepository;
import com.dxh.learninghub.service.interfac.admin.AdminReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminReviewServiceImpl implements AdminReviewService {

    ReviewRepository reviewRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_EXISTED));

        reviewRepository.delete(review);
    }
}
