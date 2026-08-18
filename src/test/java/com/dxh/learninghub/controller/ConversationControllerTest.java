package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.ConversationResponse;
import com.dxh.learninghub.dto.response.MessageResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PresignedUploadResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private AwsS3Service awsS3Service;

    @Test
    void getMyConversations_returnsInbox() throws Exception {
        when(chatService.getMyConversations()).thenReturn(List.of(
                ConversationResponse.builder()
                        .conversationId(9L)
                        .courseTitle("Java Backend")
                        .unreadCount(2)
                        .build()));

        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].conversationId").value(9))
                .andExpect(jsonPath("$.result[0].unreadCount").value(2));
    }

    @Test
    void getMessages_passesConversationAndPageable() throws Exception {
        when(chatService.getMessages(any(Long.class), any(Pageable.class)))
                .thenReturn(PageResponse.<MessageResponse>builder()
                        .pageNo(1)
                        .pageSize(20)
                        .totalElements(0)
                        .items(List.of())
                        .build());

        mockMvc.perform(get("/conversations/{conversationId}/messages", 9L)
                        .param("pageSize", "20")
                        .param("sortBy", "createdAt:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.pageSize").value(20));

        verify(chatService).getMessages(org.mockito.ArgumentMatchers.eq(9L), any(Pageable.class));
    }

    @Test
    void getMessages_whenConversationIsForbidden_returnsMappedError() throws Exception {
        when(chatService.getMessages(any(Long.class), any(Pageable.class)))
                .thenThrow(new AppException(ErrorCode.CONVERSATION_NOT_FOUND_OR_FORBIDDEN));

        mockMvc.perform(get("/conversations/{conversationId}/messages", 99L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value(ErrorCode.CONVERSATION_NOT_FOUND_OR_FORBIDDEN.getCode()));
    }

    @Test
    void getChatImageUploadUrl_returnsPresignedUrl() throws Exception {
        when(awsS3Service.generateChatUploadUrl("photo.png", 2048L))
                .thenReturn(PresignedUploadResponse.builder()
                        .uploadUrl("https://upload.example/chat")
                        .fileUrl("imagechat/photo.png")
                        .build());

        mockMvc.perform(get("/conversations/upload-url")
                        .param("fileName", "photo.png")
                        .param("fileSize", "2048"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fileUrl").value("imagechat/photo.png"));
    }

    @Test
    void markAsRead_callsService() throws Exception {
        doNothing().when(chatService).markAsRead(9L);

        mockMvc.perform(patch("/conversations/{conversationId}/read", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversation marked as read"));

        verify(chatService).markAsRead(9L);
    }

    @Test
    void createCourseConversation_returnsCreated() throws Exception {
        when(chatService.getOrCreateCourseQaConversation(3L))
                .thenReturn(ConversationResponse.builder().conversationId(9L).build());

        mockMvc.perform(post("/conversations/course/{courseId}", 3L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.conversationId").value(9));
    }
}
