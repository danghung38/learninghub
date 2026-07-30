package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.AdvertisementResponse;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.service.interfac.AdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Advertisements", description = "Public APIs for active advertisements")
public class AdvertisementController {

    AdvertisementService advertisementService;

    @Operation(summary = "Get active advertisements", description = "Return advertisements currently available for display")
    @GetMapping
    ApiResponse<List<AdvertisementResponse>> getActiveAdvertisements() {
        return ApiResponse.<List<AdvertisementResponse>>builder()
                .message("Get advertisements successfully")
                .result(advertisementService.getActiveAdvertisements())
                .build();
    }
}
