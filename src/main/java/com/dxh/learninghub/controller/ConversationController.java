package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.*;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.ChatService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Chat", description = "APIs for 1-1 conversations and message history")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    ChatService chatService;
    AwsS3Service awsS3Service;

    @Operation(summary = "Get my conversations", description = "Return conversations belonging to the current user")
    @GetMapping
    public ApiResponse<List<ConversationResponse>> getMyConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .code(HttpStatus.OK.value())
                .result(chatService.getMyConversations())
                .build();
    }

    @Operation(summary = "Get message history", description = "Return paginated messages from a conversation the current user can access")
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "createdAt:desc") String sortBy) {

        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy, "id", "createdAt");

        return ApiResponse.<PageResponse<MessageResponse>>builder()
                .code(HttpStatus.OK.value())
                .result(chatService.getMessages(conversationId, pageable))
                .build();
    }

    @Operation(summary = "Get presigned URL for chat image upload", description = "Generate a presigned URL so the client can upload an image directly to S3")
    @GetMapping("/upload-url")
    public ApiResponse<PresignedUploadResponse> getChatImageUploadUrl(
            @RequestParam String fileName,
            @RequestParam long fileSize) {

        return ApiResponse.<PresignedUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Generate chat upload URL successfully")
                .result(awsS3Service.generateChatUploadUrl(fileName, fileSize))
                .build();
    }

    @Operation(summary = "Mark conversation as read", description = "Mark all unread messages in a conversation as read")
    @PatchMapping("/{conversationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long conversationId) {
        chatService.markAsRead(conversationId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Conversation marked as read")
                .build();
    }

    @Operation(summary = "Open a support conversation", description = "Create or return the current user's support conversation")
    @PostMapping("/support")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createSupportConversation() {
        return ApiResponse.<ConversationResponse>builder()
                .code(HttpStatus.CREATED.value())
                .result(chatService.getOrCreateSupportConversation())
                .build();
    }

    @Operation(summary = "Open a course conversation", description = "Create or return the current user's course question conversation")
    @PostMapping("/course/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createCourseQaConversation(@PathVariable Long courseId) {
        return ApiResponse.<ConversationResponse>builder()
                .code(HttpStatus.CREATED.value())
                .result(chatService.getOrCreateCourseQaConversation(courseId))
                .build();
    }

    @Operation(
            summary = "Open a course student conversation",
            description = "Create or return a conversation between the course manager and an enrolled student")
    @PostMapping("/course/{courseId}/students/{studentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createCourseStudentConversation(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return ApiResponse.<ConversationResponse>builder()
                .code(HttpStatus.CREATED.value())
                .result(chatService.getOrCreateCourseStudentConversation(courseId, studentId))
                .build();
    }
}
