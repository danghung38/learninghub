package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.FavoriteRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.FavoriteResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.service.interfac.FavoriteService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Favorites", description = "APIs for managing the current user's favorite courses")
public class FavoriteController {

    FavoriteService favoriteService;

    @Operation(summary = "Add a favorite", description = "Add a course to the current user's favorites")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@Valid @RequestBody FavoriteRequest request) {

        favoriteService.createFavorite(request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create favorite successfully")
                .build();
    }


    @Operation(summary = "Get my favorites", description = "Return the current user's paginated favorite courses")
    @GetMapping
    public ApiResponse<PageResponse<FavoriteResponse>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Pageable pageable = PageUtil.createPageable(
                page, size, "createdAt:desc", "id", "createdAt", "updatedAt");

        return ApiResponse.<PageResponse<FavoriteResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get favorites successfully")
                .result(favoriteService.getMyFavorites(pageable))
                .build();
    }

    @Operation(summary = "Remove a favorite", description = "Remove a course from the current user's favorites")
    @DeleteMapping("/courses/{courseId}")
    public ApiResponse<Void> delete(@PathVariable Long courseId) {

        favoriteService.deleteFavoriteByCourseId(courseId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete favorite successfully")
                .build();
    }
}
