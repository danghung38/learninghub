package com.dxh.learninghub.controller;


import com.dxh.learninghub.dto.request.PermissionRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.PermissionResponse;
import com.dxh.learninghub.service.interfac.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Permissions", description = "APIs for administrators to manage permissions")
public class PermissionController {
    PermissionService permissionService;

    @Operation(summary = "Create a permission", description = "Create a permission for role-based access control")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request){
        return ApiResponse.<PermissionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create permission successfully")
                .result(permissionService.create(request))
                .build();
    }

    @Operation(summary = "Get all permissions", description = "Return all configured permissions")
    @GetMapping
    ApiResponse<List<PermissionResponse>> getAll(){
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    @Operation(summary = "Delete a permission", description = "Delete a permission by name")
    @DeleteMapping("/{permission}")
    ApiResponse<Void> delete(@PathVariable String permission){
        permissionService.delete(permission);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete permission successfully")
                .build();
    }
}
