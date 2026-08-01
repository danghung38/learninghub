package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.ReviewRequest;
import com.dxh.learninghub.dto.request.ReviewUpdateRequest;
import com.dxh.learninghub.dto.request.ReviewReplyRequest;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.RatingSummaryResponse;
import com.dxh.learninghub.dto.response.ReviewResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Review;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.ReviewMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.ReviewRepository;
import com.dxh.learninghub.service.interfac.ReviewService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    static List<EnrollmentStatus> REVIEWABLE_STATUSES =
            List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    ReviewRepository reviewRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewMapper reviewMapper;
    CurrentUserProvider currentUserProvider;
    NotificationService notificationService;

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse create(Long courseId, ReviewRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Course course = findApprovedCourse(courseId);

        if (!enrollmentRepository.existsByUserAndCourseAndStatusIn(user, course, REVIEWABLE_STATUSES)) {
            throw new AppException(ErrorCode.REVIEW_REQUIRES_ENROLLMENT);
        }

        if (reviewRepository.existsByUserAndCourseAndParentReviewIsNull(user, course)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewMapper.toReview(request);
        review.setUser(user);
        review.setCourse(course);
        review.setContent(request.content().trim());

        Review savedReview = reviewRepository.save(review);

        notificationService.createNotification(
                course.getAuthor(),
                user,
                "New course review",
                user.getFullName() + " reviewed \"" + course.getTitle() + "\"",
                "/courses/" + course.getId() + "/reviews");

        return reviewMapper.toReviewResponse(savedReview);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public ReviewResponse reply(Long reviewId, ReviewReplyRequest request) {
        Review parent = findReview(reviewId);
        if (parent.getParentReview() != null) {
            throw new AppException(ErrorCode.CANNOT_REPLY_TO_REPLY);
        }

        User currentUser = currentUserProvider.getCurrentUser();
        validateReplyPermission(currentUser, parent.getCourse());

        Review reply = reviewMapper.toReply(request);
        reply.setUser(currentUser);
        reply.setContent(request.content().trim());
        reply.setRating(null);
        parent.addReply(reply);

        Review savedReply = reviewRepository.save(reply);

        if (!Objects.equals(parent.getUser().getId(), currentUser.getId())) {
            notificationService.createNotification(
                    parent.getUser(),
                    currentUser,
                    "New reply to your review",
                    currentUser.getFullName() + " replied to your review",
                    "/courses/" + parent.getCourse().getId() + "/reviews");
        }

        return withReplies(reviewMapper.toReviewResponse(savedReply), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByCourse(Long courseId, Pageable pageable) {
        findApprovedCourse(courseId);

        Page<Review> reviews = reviewRepository
                .findByCourseIdAndParentReviewIsNull(courseId, pageable);

        List<Long> parentIds = reviews.getContent().stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<Review>> repliesByParentId = parentIds.isEmpty()
                ? Collections.emptyMap()
                : reviewRepository
                        .findByParentReviewIdInOrderByCreatedAtAsc(parentIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                reply -> reply.getParentReview().getId(),
                                Collectors.toList()));

        List<ReviewResponse> items = reviews.getContent().stream()
                .map(review -> {
                    ReviewResponse response = reviewMapper.toReviewResponse(review);
                    List<ReviewResponse> replies = repliesByParentId
                            .getOrDefault(review.getId(), List.of())
                            .stream()
                            .map(reply -> withReplies(
                                    reviewMapper.toReviewResponse(reply),
                                    List.of()))
                            .toList();
                    return withReplies(response, replies);
                })
                .toList();

        return PageResponse.<ReviewResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(reviews.getTotalPages())
                .totalElements(reviews.getTotalElements())
                .items(items)
                .build();
    }

    private ReviewResponse withReplies(
            ReviewResponse response,
            List<ReviewResponse> replies) {
        return ReviewResponse.builder()
                .id(response.id())
                .parentReviewId(response.parentReviewId())
                .courseId(response.courseId())
                .userId(response.userId())
                .userFullName(response.userFullName())
                .userAvatar(response.userAvatar())
                .content(response.content())
                .rating(response.rating())
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .replies(replies)
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse update(Long reviewId, ReviewUpdateRequest request) {
        Review review = findReview(reviewId);

        if (review.getParentReview() != null) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_REPLY_AS_REVIEW);
        }

        validateOwner(review);

        reviewMapper.updateFromRequest(request, review);

        Review updatedReview = reviewRepository.save(review);

        return reviewMapper.toReviewResponse(updatedReview);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteOwnReview(Long reviewId) {
        Review review = findReview(reviewId);
        validateOwner(review);
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long courseId) {
        findApprovedCourse(courseId);

        long total = reviewRepository.countByCourseIdAndParentReviewIsNull(courseId);
        double average = total == 0
                ? 5.0
                : Math.round(reviewRepository.getAverageRating(courseId) * 10.0) / 10.0;

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            distribution.put(rating, 0L);
        }
        for (Object[] row : reviewRepository.getRatingDistribution(courseId)) {
            if (row.length < 2 || !(row[0] instanceof Number rating)
                    || !(row[1] instanceof Number count)) {
                continue;
            }
            distribution.put(rating.intValue(), count.longValue());
        }

        return RatingSummaryResponse.builder()
                .courseId(courseId)
                .totalReviews(total)
                .averageRating(average)
                .ratingDistribution(distribution)
                .build();
    }

    private Course findApprovedCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }
        return course;
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findWithUserAndCourseById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_EXISTED));
    }

    private void validateOwner(Review review) {
        User currentUser = currentUserProvider.getCurrentUser();
        if (!Objects.equals(review.getUser().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_REVIEW_OWNER);
        }
    }

    private void validateReplyPermission(User user, Course course) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));
        boolean isCourseOwner = course.getAuthor() != null
                && Objects.equals(course.getAuthor().getId(), user.getId());

        if (!isAdmin && !isCourseOwner) {
            throw new AppException(ErrorCode.REVIEW_REPLY_FORBIDDEN);
        }
    }

}
