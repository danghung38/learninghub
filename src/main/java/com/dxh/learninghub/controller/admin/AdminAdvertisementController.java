package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.request.AdvertisementCreationRequest;
import com.dxh.learninghub.dto.request.AdvertisementTestSendRequest;
import com.dxh.learninghub.dto.request.AdvertisementUpdateRequest;
import com.dxh.learninghub.dto.response.AdvertisementResponse;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.service.interfac.AdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/advertisements")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Advertisements", description = "APIs for administrators to manage advertisements")
public class AdminAdvertisementController {

    AdvertisementService advertisementService;

    @Operation(summary = "Get all advertisements", description = "Return all advertisements for the administration workspace")
    @GetMapping
    ApiResponse<List<AdvertisementResponse>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean sent,
            @RequestParam(required = false) String title) {
        return ApiResponse.<List<AdvertisementResponse>>builder()
                .message("Get advertisements successfully")
                .result(advertisementService.getAllAdvertisements(active, sent, title))
                .build();
    }

    @Operation(summary = "Create an advertisement", description = "Create an advertisement with its display image")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AdvertisementResponse> create(
            @RequestPart("advertisement") @Valid AdvertisementCreationRequest request,
            @RequestPart("image") MultipartFile image) {
        return ApiResponse.<AdvertisementResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create advertisement successfully")
                .result(advertisementService.create(request, image))
                .build();
    }

    @Operation(summary = "Update an advertisement", description = "Update advertisement details and optionally replace its image")
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<AdvertisementResponse> update(
            @PathVariable Long id,
            @RequestPart("advertisement") @Valid AdvertisementUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ApiResponse.<AdvertisementResponse>builder()
                .message("Update advertisement successfully")
                .result(advertisementService.update(id, request, image))
                .build();
    }

    @Operation(summary = "Deactivate an advertisement", description = "Deactivate an advertisement so it is no longer displayed")
    @PatchMapping("/{id}/deactivate")
    ApiResponse<AdvertisementResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.<AdvertisementResponse>builder()
                .message("Deactivate advertisement successfully")
                .result(advertisementService.deactivate(id))
                .build();
    }

    @Operation(summary = "Activate an advertisement", description = "Activate an advertisement so it can be displayed during its scheduled period")
    @PatchMapping("/{id}/activate")
    ApiResponse<AdvertisementResponse> activate(@PathVariable Long id) {
        return ApiResponse.<AdvertisementResponse>builder()
                .message("Activate advertisement successfully")
                .result(advertisementService.activate(id))
                .build();
    }

    @Operation(summary = "Send an advertisement", description = "Notify all active users and send the advertisement by email")
    @PostMapping("/{id}/send")
    ApiResponse<AdvertisementResponse> send(@PathVariable Long id) {
        return ApiResponse.<AdvertisementResponse>builder()
                .message("Advertisement notification sent successfully")
                .result(advertisementService.sendNotification(id))
                .build();
    }

    @Operation(summary = "Test an advertisement", description = "Send one advertisement notification and email to one registered user")
    @PostMapping("/test-send")
    ApiResponse<Void> testSend(@Valid @RequestBody AdvertisementTestSendRequest request) {
        advertisementService.sendTestNotification(request.advertisementId(), request.email());
        return ApiResponse.<Void>builder()
                .message("Advertisement test delivery sent successfully")
                .build();
    }

    @Operation(summary = "Reset advertisement delivery state", description = "Allow the advertisement to be sent again")
    @PatchMapping("/{id}/reset-sent")
    ApiResponse<AdvertisementResponse> resetSent(@PathVariable Long id) {
        return ApiResponse.<AdvertisementResponse>builder()
                .message("Advertisement delivery state reset successfully")
                .result(advertisementService.resetSent(id))
                .build();
    }
}
