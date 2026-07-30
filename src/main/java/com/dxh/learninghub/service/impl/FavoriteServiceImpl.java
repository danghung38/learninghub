package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.FavoriteRequest;
import com.dxh.learninghub.dto.response.FavoriteResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Favorite;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.FavoriteMapper;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.FavoriteRepository;
import com.dxh.learninghub.service.interfac.FavoriteService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteServiceImpl implements FavoriteService {

    FavoriteRepository favoriteRepository;
    CourseRepository courseRepository;
    FavoriteMapper favoriteMapper;
    CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void createFavorite(FavoriteRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Course course = courseRepository.findPublicCourseById(request.courseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_AVAILABLE));

        try {
            favoriteRepository.saveAndFlush(Favorite.builder()
                    .user(user)
                    .course(course)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ALREADY_IN_FAVORITES);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<FavoriteResponse> getMyFavorites(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        Page<Favorite> favorites = favoriteRepository.findByUserAndCourseStatus(
                user,
                CourseStatus.APPROVED,
                pageable);

        return PageResponse.<FavoriteResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(favorites.getSize())
                .totalPage(favorites.getTotalPages())
                .totalElements(favorites.getTotalElements())
                .items(favorites.stream()
                        .map(favoriteMapper::toFavoriteResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public void deleteFavoriteByCourseId(Long courseId) {
        User user = currentUserProvider.getCurrentUser();
        Favorite favorite = favoriteRepository.findByUserAndCourseId(user, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_EXISTED));
        favoriteRepository.delete(favorite);
    }
}
