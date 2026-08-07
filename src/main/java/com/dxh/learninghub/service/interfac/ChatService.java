package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.ChatSendRequest;
import com.dxh.learninghub.dto.response.ConversationResponse;
import com.dxh.learninghub.dto.response.MessageResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    List<ConversationResponse> getMyConversations();

    PageResponse<MessageResponse> getMessages(Long conversationId, Pageable pageable);

    void markAsRead(Long conversationId);

    ConversationResponse getOrCreateSupportConversation();

    ConversationResponse getOrCreateCourseQaConversation(Long courseId);

    ConversationResponse getOrCreateCourseStudentConversation(Long courseId, Long studentId);

    void sendMessage(String username, ChatSendRequest request);
}
