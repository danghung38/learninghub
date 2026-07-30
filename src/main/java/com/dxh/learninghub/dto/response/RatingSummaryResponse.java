package com.dxh.learninghub.dto.response;

import lombok.Builder;

import java.io.Serializable;
import java.util.Map;

@Builder
public record RatingSummaryResponse(
    Long courseId,

    Long totalReviews,

    Double averageRating,

    Map<Integer, Long> ratingDistribution
) implements Serializable {}
