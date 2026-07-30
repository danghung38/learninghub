package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.request.ReviewRequest;
import com.dxh.learninghub.dto.request.ReviewReplyRequest;
import com.dxh.learninghub.dto.request.ReviewUpdateRequest;
import com.dxh.learninghub.dto.response.ReviewResponse;
import com.dxh.learninghub.entity.Review;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = AwsS3Service.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Review toReview(ReviewRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Review toReply(ReviewReplyRequest request);

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(
            source = "user.avatar",
            target = "userAvatar",
            qualifiedByName = "resolveFileUrl")
    @Mapping(source = "parentReview.id", target = "parentReviewId")
    @Mapping(target = "replies", ignore = true)
    ReviewResponse toReviewResponse(Review review);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "content", target = "content", qualifiedByName = "trim")
    void updateFromRequest(ReviewUpdateRequest request, @MappingTarget Review review);

    @Named("trim")
    default String trim(String value) {
        return value.trim();
    }
}
