package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.ChapterRequest;
import com.dxh.learninghub.dto.request.ChapterUpdateRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.ChapterResponse;
import com.dxh.learninghub.service.interfac.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chapters")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Chapters", description = "APIs for teachers and administrators to manage course chapters")
public class ChapterController {

    ChapterService chapterService;

    @Operation(summary = "Create a chapter", description = "Create a chapter under a course owned by the teacher")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChapterResponse> create(
            @RequestBody @Valid ChapterRequest request) {

        return ApiResponse.<ChapterResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create chapter successfully")
                .result(chapterService.create(request))
                .build();
    }

    @Operation(summary = "Update a chapter", description = "Update a chapter's title and description")
    @PatchMapping("/{id}")
    public ApiResponse<ChapterResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ChapterUpdateRequest request) {

        return ApiResponse.<ChapterResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update chapter successfully")
                .result(chapterService.update(id, request))
                .build();
    }

    @Operation(summary = "Delete a chapter", description = "Delete a chapter owned by the current teacher or as an administrator")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {

        chapterService.delete(id);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete chapter successfully")
                .build();
    }
}
