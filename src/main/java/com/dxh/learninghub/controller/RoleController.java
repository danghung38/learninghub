package com.dxh.learninghub.controller;


import com.dxh.learninghub.dto.request.RoleRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.RoleResponse;
import com.dxh.learninghub.service.interfac.RoleService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Roles", description = "APIs for administrators to manage roles")
public class RoleController {
    RoleService roleService;

    @Operation(summary = "Create a role", description = "Create a role and assign its permissions")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request){
        return ApiResponse.<RoleResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create role successfully")
                .result(roleService.create(request))
                .build();
    }

    @Operation(summary = "Get all roles", description = "Return all roles and their permissions")
    @GetMapping
    ApiResponse<List<RoleResponse>> getAll(){
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @Operation(summary = "Delete a role", description = "Delete a role by ID")
    @DeleteMapping("/{roleId}")
    ApiResponse<Void> delete(@PathVariable Long roleId){
        roleService.delete(roleId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete role successfully")
                .build();
    }
}
