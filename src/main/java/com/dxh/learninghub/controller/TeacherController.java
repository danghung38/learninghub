package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.TeacherRegisterRequest;
import com.dxh.learninghub.dto.request.TeacherUpdateRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.TeacherResponse;
import com.dxh.learninghub.service.interfac.TeacherService;
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

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teachers", description = "APIs for teacher applications and profile management")
public class TeacherController {

    TeacherService teacherService;

    @Operation(
            summary = "Register as a teacher",
            description = "Submit an authenticated user's teacher application with CV and certificate documents"
    )
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherResponse> registerTeacher(
            @RequestPart("teacher") @Valid TeacherRegisterRequest request,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart("certificate") MultipartFile certificate) {

        return ApiResponse.<TeacherResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Teacher registration submitted successfully")
                .result(teacherService.registerTeacher(request, cv, certificate))
                .build();
    }

    @Operation(
            summary = "Re-register as a teacher",
            description = "Replace a pending or rejected teacher application and submit it for review"
    )
    @PutMapping(value = "/re-register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TeacherResponse> reRegisterTeacher(
            @RequestPart("teacher") @Valid TeacherRegisterRequest request,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart("certificate") MultipartFile certificate) {

        return ApiResponse.<TeacherResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Teacher registration resubmitted successfully")
                .result(teacherService.reRegisterTeacher(request, cv, certificate))
                .build();
    }

    @Operation(
            summary = "Update teacher profile",
            description = "Update teacher information and optionally replace the CV or certificate"
    )
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TeacherResponse> updateTeacherProfile(
            @RequestPart("request") @Valid TeacherUpdateRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate) {

        return ApiResponse.<TeacherResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Teacher profile updated successfully")
                .result(teacherService.updateTeacherProfile(request, cv, certificate))
                .build();
    }

    @Operation(
            summary = "Get teacher profile",
            description = "Return the current authenticated teacher's profile information"
    )
    @GetMapping("/profile")
    public ApiResponse<TeacherResponse> getTeacherProfile() {

        return ApiResponse.<TeacherResponse>builder()
                .code(HttpStatus.OK.value())
                .result(teacherService.getTeacherProfile())
                .build();
    }

}
