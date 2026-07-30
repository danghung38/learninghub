package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
    Long id,

    Long parentReviewId,

    Long courseId,

    Long userId,

    String userFullName,

    String userAvatar,

    String content,

    Integer rating,

    LocalDateTime createdAt,

    LocalDateTime updatedAt,

    List<ReviewResponse> replies
) implements Serializable {}
