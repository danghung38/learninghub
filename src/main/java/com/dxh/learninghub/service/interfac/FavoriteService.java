package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.FavoriteRequest;
import com.dxh.learninghub.dto.response.FavoriteResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;


public interface FavoriteService {

    void createFavorite(FavoriteRequest request);

    PageResponse<FavoriteResponse> getMyFavorites(Pageable pageable);

    void deleteFavoriteByCourseId(Long courseId);
}
